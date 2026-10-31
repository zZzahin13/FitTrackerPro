package com.example.fittrackerpro.viewmodel;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.entity.PlanExerciseItem;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.repository.SessionRepository;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.repository.WorkoutRepository;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.function.Consumer;

public class WorkoutDayViewModel extends AndroidViewModel {

    private final WorkoutRepository repository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final String userId;
    private LiveData<List<PlanExerciseItem>> exercises;

    // Timer state lives here, so it survives screen rotation
    private boolean running = false;
    private long runStartedAt = 0;   // clock reading when the current run began
    private long accumulatedMs = 0;  // time from earlier runs (before pauses)
    private long restEndsAt = 0;     // clock reading when the rest ends, 0 = no rest

    public WorkoutDayViewModel(@NonNull Application application) {
        super(application);
        repository = new WorkoutRepository(application);
        sessionRepository = new SessionRepository(application);
        userRepository = new UserRepository(application);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";
    }

    public void init(String planId, int dayOfWeek) {
        if (exercises == null) {
            exercises = repository.observeDayExercises(planId, dayOfWeek);
        }
    }

    public LiveData<List<PlanExerciseItem>> getExercises() {
        return exercises;
    }

    public void setCompleted(PlanExerciseItem item, boolean completed) {
        repository.setExerciseCompleted(item.planExerciseId, completed);
    }

    // ---------- Workout stopwatch ----------

    public void toggleTimer() {
        long now = SystemClock.elapsedRealtime();
        if (running) {
            accumulatedMs += now - runStartedAt;
            running = false;
        } else {
            runStartedAt = now;
            running = true;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean hasStarted() {
        return running || accumulatedMs > 0;
    }

    public long getElapsedMs() {
        return accumulatedMs + (running ? SystemClock.elapsedRealtime() - runStartedAt : 0);
    }

    // ---------- Rest countdown ----------

    public void startRest(int seconds) {
        restEndsAt = SystemClock.elapsedRealtime() + seconds * 1000L;
    }

    public void skipRest() {
        restEndsAt = 0;
    }

    public boolean isRestActive() {
        return restEndsAt != 0;
    }

    public long getRestRemainingMs() {
        return Math.max(0, restEndsAt - SystemClock.elapsedRealtime());
    }

    // ---------- Finish ----------

    public void finishWorkout(String planId, boolean allDone, Consumer<Double> onSaved) {
        if (running) {
            toggleTimer();   // pause so the final time is fixed
        }
        int minutes = Math.max(1, (int) Math.round(getElapsedMs() / 60000.0));

        userRepository.getProfileOnce(userId, profile -> {
            double weight = profile != null ? profile.currentWeightKg : 70;
            double kcal = CalorieCalculator.workoutCalories(weight, minutes);

            WorkoutSession s = new WorkoutSession();
            s.userId = userId;
            s.planId = planId;
            s.date = System.currentTimeMillis();
            s.durationMinutes = minutes;
            s.caloriesBurned = kcal;
            s.completed = allDone;
            s.isSynced = false;
            s.updatedAt = System.currentTimeMillis();

            sessionRepository.saveSession(s, () -> onSaved.accept(kcal));
        });
    }
}