package com.example.fittrackerpro.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.database.dao.ActivityLogDao;
import com.example.fittrackerpro.database.dao.UserProfileDao;
import com.example.fittrackerpro.database.dao.WeightHistoryDao;
import com.example.fittrackerpro.database.dao.WorkoutSessionDao;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.utils.SyncManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Day 8B: WorkManager background worker that uploads unsynced Room entities
 * (UserProfile, ActivityLog, WorkoutSession, WeightHistory) to Firebase Firestore
 * under users/{userId}/... and marks them isSynced = true in Room.
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

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userDoc = firestore.collection("users").document(uid);

        int syncedItems = 0;

        try {
            // 1. Sync UserProfile if modified (isSynced == false)
            UserProfile profile = profileDao.getProfileOnce(uid);
            if (profile != null && !profile.isSynced) {
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

                Tasks.await(userDoc.set(profileMap, SetOptions.merge()), 15, TimeUnit.SECONDS);
                profileDao.markSynced(uid);
                syncedItems++;
            }

            // 2. Sync unsynced ActivityLog rows
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

                Tasks.await(
                        userDoc.collection("activity_logs").document(log.activityId).set(map),
                        15, TimeUnit.SECONDS);
                activityDao.markSynced(log.activityId);
                syncedItems++;
            }

            // 3. Sync unsynced WorkoutSession rows
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

                Tasks.await(
                        userDoc.collection("workout_sessions").document(s.sessionId).set(map),
                        15, TimeUnit.SECONDS);
                sessionDao.markSynced(s.sessionId);
                syncedItems++;
            }

            // 4. Sync unsynced WeightHistory rows
            List<WeightHistory> unsyncedWeights = weightDao.getUnsynced(uid);
            for (WeightHistory w : unsyncedWeights) {
                Map<String, Object> map = new HashMap<>();
                map.put("recordId", w.recordId);
                map.put("userId", w.userId);
                map.put("weightKg", w.weightKg);
                map.put("date", w.date);
                map.put("updatedAt", w.updatedAt);

                Tasks.await(
                        userDoc.collection("weight_history").document(w.recordId).set(map),
                        15, TimeUnit.SECONDS);
                weightDao.markSynced(w.recordId);
                syncedItems++;
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
