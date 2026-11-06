package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fittrackerpro.repository.UserRepository;
import com.example.fittrackerpro.repository.WorkoutXRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AnimationViewModel extends AndroidViewModel {

    private final WorkoutXRepository repository;
    private final UserRepository userRepository;
    private final MutableLiveData<WorkoutXRepository.AnimationResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private boolean started = false;
    private String exerciseId = "";
    private String exerciseName = "";

    public AnimationViewModel(@NonNull Application application) {
        super(application);
        repository = new WorkoutXRepository(application);
        userRepository = new UserRepository(application);
    }

    public LiveData<WorkoutXRepository.AnimationResult> getResult() {
        return result;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    // Safe to call again after rotation: the lookup only happens once
    public void start(String exerciseId, String exerciseName) {
        if (started) return;
        started = true;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        loading.setValue(true);

        String saved = repository.getPreferredModel();
        if (saved != null) {
            load(saved);
            return;
        }

        // First time: start with the model that matches the assessment gender
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            load("male");
            return;
        }
        userRepository.getProfileOnce(user.getUid(), profile -> {
            boolean female = profile != null && "Female".equals(profile.gender);
            load(female ? "female" : "male");
        });
    }

    private void load(String model) {
        repository.load(exerciseId, exerciseName, model, r -> {
            loading.setValue(false);
            result.setValue(r);
        });
    }

    public void switchModel() {
        loading.setValue(true);
        repository.switchModel(exerciseId, r -> {
            loading.setValue(false);
            result.setValue(r);
        });
    }
}