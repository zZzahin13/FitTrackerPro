package com.example.fittrackerpro.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.fittrackerpro.activities.AssessmentActivity;
import com.example.fittrackerpro.activities.LoginActivity;
import com.example.fittrackerpro.activities.MainActivity;
import com.example.fittrackerpro.database.ExerciseSeeder;
import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.FoodSeeder;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WaterLog;
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.repository.WorkoutRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

/**
 * Routes the user on app startup or after Login:
 *  1. If not signed in -> LoginActivity.
 *  2. If local Room database already has a completed UserProfile -> MainActivity (and pulls any missing cloud logs).
 *  3. If local Room database is empty (e.g., fresh install or new phone), fetches users/{uid}
 *     and its subcollections from Firebase Firestore, restores them into Room, and opens MainActivity!
 */
public class StartupRouter {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void route(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            open(activity, LoginActivity.class);
            return;
        }

        final Context appContext = activity.getApplicationContext();
        final String uid = user.getUid();

        FitTrackerDatabase.dbExecutor.execute(() -> {
            FitTrackerDatabase db = FitTrackerDatabase.getInstance(appContext);
            UserProfile local = db.userProfileDao().getProfileOnce(uid);

            if (local != null && local.profileCompleted) {
                // Local profile exists: open Dashboard right away and restore any cloud logs in background
                MAIN.post(() -> open(activity, MainActivity.class));
                restoreSubcollectionsFromCloud(appContext, uid);
                return;
            }

            // Local profile is missing (e.g. fresh install / new device): check Firebase Firestore first!
            UserProfile cloudProfile = fetchAndRestoreFromFirestore(appContext, uid);
            final boolean done = (cloudProfile != null && cloudProfile.profileCompleted);
            MAIN.post(() -> open(activity, done ? MainActivity.class : AssessmentActivity.class));
        });
    }

    private static UserProfile fetchAndRestoreFromFirestore(Context appContext, String uid) {
        try {
            FitTrackerDatabase db = FitTrackerDatabase.getInstance(appContext);
            DocumentReference userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid);

            DocumentSnapshot snap = Tasks.await(userDoc.get(), 6, TimeUnit.SECONDS);
            if (snap == null || !snap.exists()) {
                return null;
            }

            Boolean completed = snap.getBoolean("profileCompleted");
            if (completed == null || !completed) {
                return null;
            }

            UserProfile p = new UserProfile();
            p.userId = uid;
            p.name = getString(snap, "name", "Athlete");
            p.age = getInt(snap, "age", 24);
            p.gender = getString(snap, "gender", "Male");
            p.heightCm = getDouble(snap, "heightCm", 170.0);
            p.currentWeightKg = getDouble(snap, "currentWeightKg", 70.0);
            p.targetWeightKg = getDouble(snap, "targetWeightKg", 68.0);
            p.fitnessGoal = getString(snap, "fitnessGoal", "General Fitness");
            p.experience = getString(snap, "experience", "Beginner");
            p.activityLevel = getString(snap, "activityLevel", "Moderately Active");
            p.dietaryPreference = getString(snap, "dietaryPreference", "Balanced");
            p.workoutLocation = getString(snap, "workoutLocation", "Gym");
            p.workoutMinutes = getInt(snap, "workoutMinutes", 45);
            p.workoutDaysPerWeek = getInt(snap, "workoutDaysPerWeek", 4);
            p.limitations = getString(snap, "limitations", "None");
            p.profileCompleted = true;
            p.isSynced = true;
            p.updatedAt = getLong(snap, "updatedAt", System.currentTimeMillis());

            db.userProfileDao().upsert(p);

            // Restore subcollections (activity_logs, workout_sessions, weight_history, food_logs, water_logs)
            restoreSubcollectionsFromCloud(appContext, uid);

            // Regenerate 7-day workout schedule for the restored profile so Workout & Home tabs are populated
            new WorkoutRepository(appContext).createPlan(p, 1, null);

            return p;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void restoreSubcollectionsFromCloud(Context appContext, String uid) {
        try {
            FitTrackerDatabase db = FitTrackerDatabase.getInstance(appContext);

            // Ensure food & exercise catalogs exist before inserting foreign-keyed logs
            if (db.mealDao().count() == 0) {
                db.mealDao().insertAll(FoodSeeder.createFoods());
            }
            if (db.exerciseDao().count() == 0) {
                db.exerciseDao().insertAll(ExerciseSeeder.createExercises());
            }

            DocumentReference userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid);

            // 1. Restore activity_logs
            QuerySnapshot actSnap = Tasks.await(
                    userDoc.collection("activity_logs").get(), 5, TimeUnit.SECONDS);
            if (actSnap != null) {
                for (DocumentSnapshot d : actSnap.getDocuments()) {
                    ActivityLog log = new ActivityLog();
                    log.activityId = d.getId();
                    log.userId = uid;
                    log.activityType = getString(d, "activityType", "Walking");
                    log.durationMinutes = getInt(d, "durationMinutes", 20);
                    log.distanceKm = getDouble(d, "distanceKm", 0.0);
                    log.steps = getInt(d, "steps", 0);
                    log.caloriesBurned = getDouble(d, "caloriesBurned", 0.0);
                    log.date = getLong(d, "date", System.currentTimeMillis());
                    log.updatedAt = getLong(d, "updatedAt", log.date);
                    log.isSynced = true;
                    db.activityLogDao().insert(log);
                }
            }

            // 2. Restore workout_sessions
            QuerySnapshot sessSnap = Tasks.await(
                    userDoc.collection("workout_sessions").get(), 5, TimeUnit.SECONDS);
            if (sessSnap != null) {
                for (DocumentSnapshot d : sessSnap.getDocuments()) {
                    WorkoutSession s = new WorkoutSession();
                    s.sessionId = d.getId();
                    s.userId = uid;
                    s.planId = null; // keep null so foreign key to workout_plans never fails on restore
                    s.date = getLong(d, "date", System.currentTimeMillis());
                    s.durationMinutes = getInt(d, "durationMinutes", 30);
                    s.caloriesBurned = getDouble(d, "caloriesBurned", 150.0);
                    Boolean comp = d.getBoolean("completed");
                    s.completed = comp == null || comp;
                    s.updatedAt = getLong(d, "updatedAt", s.date);
                    s.isSynced = true;
                    db.workoutSessionDao().insert(s);
                }
            }

            // 3. Restore weight_history
            QuerySnapshot weightSnap = Tasks.await(
                    userDoc.collection("weight_history").get(), 5, TimeUnit.SECONDS);
            if (weightSnap != null) {
                for (DocumentSnapshot d : weightSnap.getDocuments()) {
                    WeightHistory w = new WeightHistory();
                    w.recordId = d.getId();
                    w.userId = uid;
                    w.weightKg = getDouble(d, "weightKg", 70.0);
                    w.date = getLong(d, "date", System.currentTimeMillis());
                    w.updatedAt = getLong(d, "updatedAt", w.date);
                    w.isSynced = true;
                    db.weightHistoryDao().insert(w);
                }
            }

            // 4. Restore food_logs
            QuerySnapshot foodSnap = Tasks.await(
                    userDoc.collection("food_logs").get(), 5, TimeUnit.SECONDS);
            if (foodSnap != null) {
                for (DocumentSnapshot d : foodSnap.getDocuments()) {
                    try {
                        FoodLog f = new FoodLog();
                        f.logId = d.getId();
                        f.userId = uid;
                        f.mealId = getString(d, "mealId", "");
                        f.quantity = getDouble(d, "quantity", 1.0);
                        f.date = getLong(d, "date", System.currentTimeMillis());
                        f.mealType = getString(d, "mealType", "Breakfast");
                        f.totalCalories = getDouble(d, "totalCalories", 0.0);
                        f.updatedAt = getLong(d, "updatedAt", f.date);
                        f.isSynced = true;
                        if (!f.mealId.isEmpty()) {
                            db.foodLogDao().insert(f);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 5. Restore water_logs
            QuerySnapshot waterSnap = Tasks.await(
                    userDoc.collection("water_logs").get(), 5, TimeUnit.SECONDS);
            if (waterSnap != null) {
                for (DocumentSnapshot d : waterSnap.getDocuments()) {
                    WaterLog wl = new WaterLog();
                    wl.waterId = d.getId();
                    wl.userId = uid;
                    wl.amountMl = getInt(d, "amountMl", 250);
                    wl.date = getLong(d, "date", System.currentTimeMillis());
                    wl.updatedAt = getLong(d, "updatedAt", wl.date);
                    wl.isSynced = true;
                    db.waterDao().insert(wl);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String getString(DocumentSnapshot d, String key, String def) {
        String v = d.getString(key);
        return v != null ? v : def;
    }

    private static int getInt(DocumentSnapshot d, String key, int def) {
        Long v = d.getLong(key);
        return v != null ? v.intValue() : def;
    }

    private static long getLong(DocumentSnapshot d, String key, long def) {
        Long v = d.getLong(key);
        return v != null ? v : def;
    }

    private static double getDouble(DocumentSnapshot d, String key, double def) {
        Double v = d.getDouble(key);
        return v != null ? v : def;
    }

    private static void open(Activity activity, Class<?> target) {
        Intent i = new Intent(activity, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
    }
}
