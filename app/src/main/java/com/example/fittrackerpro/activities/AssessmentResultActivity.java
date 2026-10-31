package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.databinding.ActivityAssessmentResultBinding;
import com.example.fittrackerpro.utils.BMIUtil;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.example.fittrackerpro.viewmodel.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class AssessmentResultActivity extends AppCompatActivity {

    private ActivityAssessmentResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssessmentResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getProfile(user.getUid()).observe(this, this::showResults);

        binding.btnContinue.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void showResults(UserProfile p) {
        if (p == null) return;

        double bmi = BMIUtil.bmi(p.currentWeightKg, p.heightCm);
        binding.tvBmi.setText(String.format(Locale.getDefault(), "%.1f", bmi));
        binding.tvBmiCategory.setText(BMIUtil.category(bmi, p.age));

        CalorieCalculator.Targets t = CalorieCalculator.calculate(p);
        binding.tvTarget.setText(String.valueOf(Math.round(t.targetCalories)));
        binding.tvBmr.setText("BMR (calories at rest): " + Math.round(t.bmr) + " kcal");
        binding.tvTdee.setText("TDEE (with daily activity): " + Math.round(t.tdee) + " kcal");
        binding.tvProtein.setText("Protein: " + Math.round(t.proteinG) + " g");
        binding.tvCarbs.setText("Carbohydrates: " + Math.round(t.carbsG) + " g");
        binding.tvFat.setText("Fat: " + Math.round(t.fatG) + " g");
    }
}