package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.databinding.ActivityMainBinding;
import com.example.fittrackerpro.fragments.PlaceholderFragment;
import com.example.fittrackerpro.fragments.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentFor(item.getItemId());
            if (fragment == null) {
                return false;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        });

        // After a rotation Android restores the fragment by itself,
        // so we only pick Home on a fresh start.
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    // if/else instead of switch: resource IDs are not compile-time constants in newer Gradle plugins
    private Fragment fragmentFor(int id) {
        if (id == R.id.nav_home) {
            return PlaceholderFragment.newInstance("Home", "Dashboard arrives on Day 7");
        } else if (id == R.id.nav_workout) {
            return PlaceholderFragment.newInstance("Workout", "Plans and exercises arrive on Days 3 and 4");
        } else if (id == R.id.nav_nutrition) {
            return PlaceholderFragment.newInstance("Nutrition", "Food logging arrives on Day 5");
        } else if (id == R.id.nav_activity) {
            return PlaceholderFragment.newInstance("Activity", "Tracking arrives on Day 6");
        } else if (id == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
    }
}