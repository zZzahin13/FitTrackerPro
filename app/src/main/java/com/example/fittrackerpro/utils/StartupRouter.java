package com.example.fittrackerpro.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.fittrackerpro.activities.AssessmentActivity;
import com.example.fittrackerpro.activities.LoginActivity;
import com.example.fittrackerpro.activities.MainActivity;
import com.example.fittrackerpro.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.fittrackerpro.utils.StartupRouter;
public class StartupRouter {

    public static void route(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            open(activity, LoginActivity.class);
            return;
        }
        new UserRepository(activity).getProfileOnce(user.getUid(), profile -> {
            boolean done = profile != null && profile.profileCompleted;
            open(activity, done ? MainActivity.class : AssessmentActivity.class);
        });
    }

    private static void open(Activity activity, Class<?> target) {
        Intent i = new Intent(activity, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
    }
}