package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.repository.WorkoutRepository;
import com.example.fittrackerpro.utils.DateUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WorkoutViewModel extends AndroidViewModel {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final String userId;
    private final LiveData<WorkoutPlan> plan;
    private final LiveData<List<DaySummary>> summaries;

    public WorkoutViewModel(@NonNull Application application) {
        super(application);
        workoutRepository = new WorkoutRepository(application);
        userRepository = new UserRepository(application);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";

        plan = workoutRepository.observeActivePlan(userId);
        // Whenever the plan changes, load the day summaries of the new plan
        summaries = Transformations.switchMap(plan, p -> summariesFor(p));
    }

    private LiveData<List<DaySummary>> summariesFor(WorkoutPlan p) {
        if (p == null) {
            MutableLiveData<List<DaySummary>> empty = new MutableLiveData<>();
            empty.setValue(new ArrayList<>());
            return empty;
        }
        return workoutRepository.observeDaySummaries(p.planId, DateUtil.startOfWeek());
    }

    public LiveData<WorkoutPlan> getPlan() {
        return plan;
    }

    public LiveData<List<DaySummary>> getSummaries() {
        return summaries;
    }

    // result.accept(false) means the assessment is not finished yet
    public void createPlan(int months, Consumer<Boolean> result) {
        userRepository.getProfileOnce(userId, profile -> {
            if (profile == null || !profile.profileCompleted) {
                result.accept(false);
                return;
            }
            workoutRepository.createPlan(profile, months, () -> result.accept(true));
        });
    }
}