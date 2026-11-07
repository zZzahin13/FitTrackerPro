package com.example.fittrackerpro.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.ActivityLogDao;
import com.example.fittrackerpro.database.dao.FoodLogDao;
import com.example.fittrackerpro.database.dao.UserProfileDao;
import com.example.fittrackerpro.database.dao.WaterDao;
import com.example.fittrackerpro.database.dao.WeightHistoryDao;
import com.example.fittrackerpro.database.dao.WorkoutSessionDao;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.FoodLog;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WaterLog;
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.utils.SyncManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Day 8B: WorkManager background worker that uploads unsynced Room entities
 * (UserProfile, ActivityLog, WorkoutSession, WeightHistory, FoodLog, WaterLog)
 * to Firebase Firestore in a single atomic WriteBatch under users/{userId}/...
 * and mirrors live user status to Firebase Realtime Database.
 */
public class SyncWorker extends Worker {

    public static final String KEY_SYNCED_COUNT = "key_synced_count";
    public static final String KEY_ERROR_MESSAGE = "key_error_message";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.failure(new Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, "Not signed in")
                    .build());
        }

        String uid = user.getUid();
        FitTrackerDatabase db = FitTrackerDatabase.getInstance(getApplicationContext());
        UserProfileDao profileDao = db.userProfileDao();
        ActivityLogDao activityDao = db.activityLogDao();
        WorkoutSessionDao sessionDao = db.workoutSessionDao();
        WeightHistoryDao weightDao = db.weightHistoryDao();
        FoodLogDao foodLogDao = db.foodLogDao();
        WaterDao waterDao = db.waterDao();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userDoc = firestore.collection("users").document(uid);
        WriteBatch batch = firestore.batch();

        int syncedItems = 0;

        try {
            // 1. Stage UserProfile if modified (isSynced == false)
            UserProfile profile = profileDao.getProfileOnce(uid);
            boolean hasProfileToSync = (profile != null && !profile.isSynced);
            if (hasProfileToSync) {
                Map<String, Object> profileMap = new HashMap<>();
                profileMap.put("userId", profile.userId);
                profileMap.put("name", profile.name);
                profileMap.put("age", profile.age);
                profileMap.put("gender", profile.gender);
                profileMap.put("heightCm", profile.heightCm);
                profileMap.put("currentWeightKg", profile.currentWeightKg);
                profileMap.put("targetWeightKg", profile.targetWeightKg);
                profileMap.put("fitnessGoal", profile.fitnessGoal);
                profileMap.put("experience", profile.experience);
                profileMap.put("activityLevel", profile.activityLevel);
                profileMap.put("dietaryPreference", profile.dietaryPreference);
                profileMap.put("workoutLocation", profile.workoutLocation);
                profileMap.put("workoutMinutes", profile.workoutMinutes);
                profileMap.put("workoutDaysPerWeek", profile.workoutDaysPerWeek);
                profileMap.put("limitations", profile.limitations);
                profileMap.put("profileCompleted", profile.profileCompleted);
                profileMap.put("updatedAt", profile.updatedAt);

                batch.set(userDoc, profileMap, SetOptions.merge());
                syncedItems++;
            }

            // 2. Stage unsynced ActivityLog rows
            List<ActivityLog> unsyncedActivities = activityDao.getUnsynced(uid);
            for (ActivityLog log : unsyncedActivities) {
                Map<String, Object> map = new HashMap<>();
                map.put("activityId", log.activityId);
                map.put("userId", log.userId);
                map.put("activityType", log.activityType);
                map.put("durationMinutes", log.durationMinutes);
                map.put("distanceKm", log.distanceKm);
                map.put("steps", log.steps);
                map.put("caloriesBurned", log.caloriesBurned);
                map.put("date", log.date);
                map.put("updatedAt", log.updatedAt);

                batch.set(userDoc.collection("activity_logs").document(log.activityId), map);
                syncedItems++;
            }

            // 3. Stage unsynced WorkoutSession rows
            List<WorkoutSession> unsyncedSessions = sessionDao.getUnsynced(uid);
            for (WorkoutSession s : unsyncedSessions) {
                Map<String, Object> map = new HashMap<>();
                map.put("sessionId", s.sessionId);
                map.put("userId", s.userId);
                map.put("planId", s.planId);
                map.put("date", s.date);
                map.put("durationMinutes", s.durationMinutes);
                map.put("caloriesBurned", s.caloriesBurned);
                map.put("completed", s.completed);
                map.put("updatedAt", s.updatedAt);

                batch.set(userDoc.collection("workout_sessions").document(s.sessionId), map);
                syncedItems++;
            }

            // 4. Stage unsynced WeightHistory rows
            List<WeightHistory> unsyncedWeights = weightDao.getUnsynced(uid);
            for (WeightHistory w : unsyncedWeights) {
                Map<String, Object> map = new HashMap<>();
                map.put("recordId", w.recordId);
                map.put("userId", w.userId);
                map.put("weightKg", w.weightKg);
                map.put("date", w.date);
                map.put("updatedAt", w.updatedAt);

                batch.set(userDoc.collection("weight_history").document(w.recordId), map);
                syncedItems++;
            }

            // 5. Stage unsynced FoodLog rows
            List<FoodLog> unsyncedFoods = foodLogDao.getUnsynced(uid);
            for (FoodLog f : unsyncedFoods) {
                Map<String, Object> map = new HashMap<>();
                map.put("logId", f.logId);
                map.put("userId", f.userId);
                map.put("mealId", f.mealId);
                map.put("quantity", f.quantity);
                map.put("date", f.date);
                map.put("mealType", f.mealType);
                map.put("totalCalories", f.totalCalories);
                map.put("updatedAt", f.updatedAt);

                batch.set(userDoc.collection("food_logs").document(f.logId), map);
                syncedItems++;
            }

            // 6. Stage unsynced WaterLog rows
            List<WaterLog> unsyncedWaters = waterDao.getUnsynced(uid);
            for (WaterLog wl : unsyncedWaters) {
                Map<String, Object> map = new HashMap<>();
                map.put("waterId", wl.waterId);
                map.put("userId", wl.userId);
                map.put("amountMl", wl.amountMl);
                map.put("date", wl.date);
                map.put("updatedAt", wl.updatedAt);

                batch.set(userDoc.collection("water_logs").document(wl.waterId), map);
                syncedItems++;
            }

            if (syncedItems > 0) {
                Task<Void> commitTask = batch.commit();
                try {
                    Tasks.await(commitTask, 6, TimeUnit.SECONDS);
                } catch (TimeoutException ignored) {
                    // Firestore has queued the batch locally in its offline persistence cache
                }

                if (hasProfileToSync) {
                    profileDao.markSynced(uid);
                }
                for (ActivityLog log : unsyncedActivities) {
                    activityDao.markSynced(log.activityId);
                }
                for (WorkoutSession s : unsyncedSessions) {
                    sessionDao.markSynced(s.sessionId);
                }
                for (WeightHistory w : unsyncedWeights) {
                    weightDao.markSynced(w.recordId);
                }
                for (FoodLog f : unsyncedFoods) {
                    foodLogDao.markSynced(f.logId);
                }
                for (WaterLog wl : unsyncedWaters) {
                    waterDao.markSynced(wl.waterId);
                }
            }

            // 7. Mirror live user status & sync heartbeat to Firebase Realtime Database
            try {
                Map<String, Object> rtdbStatus = new HashMap<>();
                rtdbStatus.put("userId", uid);
                rtdbStatus.put("name", (profile != null && profile.name != null) ? profile.name : "Athlete");
                rtdbStatus.put("fitnessGoal", (profile != null && profile.fitnessGoal != null) ? profile.fitnessGoal : "General Fitness");
                rtdbStatus.put("currentWeightKg", profile != null ? profile.currentWeightKg : 0.0);
                rtdbStatus.put("lastSyncedAt", System.currentTimeMillis());
                rtdbStatus.put("lastBatchSyncedCount", syncedItems);

                FirebaseDatabase.getInstance()
                        .getReference("user_status")
                        .child(uid)
                        .setValue(rtdbStatus);
            } catch (Exception ignored) {
            }

            SyncManager.recordSyncSuccess(getApplicationContext(), syncedItems);
            return Result.success(new Data.Builder()
                    .putInt(KEY_SYNCED_COUNT, syncedItems)
                    .build());

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Cloud sync error";
            SyncManager.recordSyncError(getApplicationContext(), msg);
            return Result.failure(new Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, msg)
                    .build());
        }
    }
}
