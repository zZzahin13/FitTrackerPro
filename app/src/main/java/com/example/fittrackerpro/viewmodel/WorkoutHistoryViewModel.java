package com.example.fittrackerpro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.repository.SessionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class WorkoutHistoryViewModel extends AndroidViewModel {

    private final SessionRepository repository;
    private final LiveData<List<WorkoutSession>> sessions;

    public WorkoutHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new SessionRepository(application);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        sessions = repository.observeSessions(user != null ? user.getUid() : "");
    }

    public LiveData<List<WorkoutSession>> getSessions() {
        return sessions;
    }

    public void delete(WorkoutSession session) {
        repository.deleteSession(session);
    }

    public void restore(WorkoutSession session) {
        repository.saveSession(session, null);
    }
}