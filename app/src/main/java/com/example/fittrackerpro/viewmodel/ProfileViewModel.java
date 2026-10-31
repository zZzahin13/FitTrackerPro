package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.repository.UserRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository repository;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public LiveData<UserProfile> getProfile(String userId) {
        return repository.observeProfile(userId);
    }

    public void save(UserProfile profile, Runnable onSaved) {
        repository.saveProfile(profile, onSaved);
    }
}