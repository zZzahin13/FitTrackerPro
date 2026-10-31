package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.databinding.ActivityAssessmentBinding;
import com.example.fittrackerpro.utils.BMIUtil;
import com.example.fittrackerpro.viewmodel.ProfileViewModel;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AssessmentActivity extends AppCompatActivity {

    private ActivityAssessmentBinding binding;
    private ProfileViewModel viewModel;
    private String userId;
    private boolean prefilled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssessmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupDropdown(binding.acGender, R.array.genders);
        setupDropdown(binding.acGoal, R.array.fitness_goals);
        setupDropdown(binding.acExperience, R.array.experience_levels);
        setupDropdown(binding.acActivity, R.array.activity_levels);
        setupDropdown(binding.acLocation, R.array.workout_locations);
        setupDropdown(binding.acDuration, R.array.workout_durations);
        setupDropdown(binding.acDays, R.array.workout_days);
        setupDropdown(binding.acDiet, R.array.dietary_preferences);

        if (user.getDisplayName() != null) {
            binding.etName.setText(user.getDisplayName());
        }

        // Edit mode: if a profile already exists, fill the form once
        viewModel.getProfile(userId).observe(this, profile -> {
            if (profile != null && !prefilled && savedInstanceState == null) {
                fillForm(profile);
            }
            prefilled = true;
        });

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupDropdown(AutoCompleteTextView view, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_list_item_1);
        view.setAdapter(adapter);
    }

    private void fillForm(UserProfile p) {
        binding.etName.setText(p.name);
        binding.etAge.setText(String.valueOf(p.age));
        binding.etHeight.setText(fmt(p.heightCm));
        binding.etWeight.setText(fmt(p.currentWeightKg));
        binding.etTarget.setText(fmt(p.targetWeightKg));
        binding.etLimitations.setText(p.limitations);

        // false = do not filter the dropdown list while setting the text
        binding.acGender.setText("Not specified".equals(p.gender) ? "" : p.gender, false);
        binding.acGoal.setText(p.fitnessGoal, false);
        binding.acExperience.setText(p.experience, false);
        binding.acActivity.setText(p.activityLevel, false);
        binding.acLocation.setText(p.workoutLocation, false);
        binding.acDuration.setText(String.valueOf(p.workoutMinutes), false);
        binding.acDays.setText(String.valueOf(p.workoutDaysPerWeek), false);
        binding.acDiet.setText(p.dietaryPreference, false);
    }

    private void saveProfile() {
        clearErrors();

        String name = text(binding.etName);
        int age = parseInt(text(binding.etAge));
        double height = parseDouble(text(binding.etHeight));
        double weight = parseDouble(text(binding.etWeight));
        double target = parseDouble(text(binding.etTarget));

        if (name.isEmpty()) {
            binding.tilName.setError("Enter your name");
            return;
        }
        if (age < 13 || age > 100) {
            binding.tilAge.setError("Enter an age between 13 and 100");
            return;
        }
        if (Double.isNaN(height) || height < 100 || height > 250) {
            binding.tilHeight.setError("Enter a height between 100 and 250 cm");
            return;
        }
        if (Double.isNaN(weight) || weight < 30 || weight > 300) {
            binding.tilWeight.setError("Enter a weight between 30 and 300 kg");
            return;
        }
        if (Double.isNaN(target) || target < 30 || target > 300) {
            binding.tilTarget.setError("Enter a target between 30 and 300 kg");
            return;
        }
        // Safety: do not accept a target that would be underweight for an adult
        if (age >= 18 && BMIUtil.bmi(target, height) < 18.5) {
            binding.tilTarget.setError("This target would be underweight (BMI below 18.5). "
                    + "Please choose a healthier target or ask a professional.");
            return;
        }
        if (!hasChoice(binding.tilGoal, binding.acGoal)
                || !hasChoice(binding.tilExperience, binding.acExperience)
                || !hasChoice(binding.tilActivity, binding.acActivity)
                || !hasChoice(binding.tilLocation, binding.acLocation)
                || !hasChoice(binding.tilDuration, binding.acDuration)
                || !hasChoice(binding.tilDays, binding.acDays)
                || !hasChoice(binding.tilDiet, binding.acDiet)) {
            return;
        }

        String gender = text(binding.acGender);

        UserProfile p = new UserProfile();
        p.userId = userId;
        p.name = name;
        p.age = age;
        p.gender = gender.isEmpty() ? "Not specified" : gender;
        p.heightCm = height;
        p.currentWeightKg = weight;
        p.targetWeightKg = target;
        p.fitnessGoal = text(binding.acGoal);
        p.experience = text(binding.acExperience);
        p.activityLevel = text(binding.acActivity);
        p.workoutLocation = text(binding.acLocation);
        p.workoutMinutes = parseInt(text(binding.acDuration));
        p.workoutDaysPerWeek = parseInt(text(binding.acDays));
        p.dietaryPreference = text(binding.acDiet);
        p.limitations = text(binding.etLimitations);
        p.profileCompleted = true;

        binding.btnSave.setEnabled(false);
        viewModel.save(p, () -> {
            startActivity(new Intent(this, AssessmentResultActivity.class));
            finish();
        });
    }

    private boolean hasChoice(TextInputLayout layout, AutoCompleteTextView view) {
        if (text(view).isEmpty()) {
            layout.setError("Please choose an option");
            return false;
        }
        return true;
    }

    private void clearErrors() {
        binding.tilName.setError(null);
        binding.tilAge.setError(null);
        binding.tilHeight.setError(null);
        binding.tilWeight.setError(null);
        binding.tilTarget.setError(null);
        binding.tilGoal.setError(null);
        binding.tilExperience.setError(null);
        binding.tilActivity.setError(null);
        binding.tilLocation.setError(null);
        binding.tilDuration.setError(null);
        binding.tilDays.setError(null);
        binding.tilDiet.setError(null);
    }

    private String text(EditText view) {
        return view.getText().toString().trim();
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    // 70.0 -> "70", 70.5 -> "70.5"
    private String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}