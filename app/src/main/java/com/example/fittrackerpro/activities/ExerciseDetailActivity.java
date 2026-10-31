package com.example.fittrackerpro.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.databinding.ActivityExerciseDetailBinding;
import com.example.fittrackerpro.viewmodel.ExerciseViewModel;

public class ExerciseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE_ID = "exercise_id";

    private ActivityExerciseDetailBinding binding;
    private ExerciseViewModel viewModel;
    private ExerciseItem current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        if (exerciseId == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);
        viewModel.getItem(exerciseId).observe(this, item -> {
            if (item == null) return;
            current = item;
            show(item);
        });

        binding.btnFavorite.setOnClickListener(v -> {
            if (current != null) {
                viewModel.toggleFavorite(current);
            }
        });
    }

    private void show(ExerciseItem item) {
        Exercise e = item.exercise;
        binding.tvName.setText(e.name);
        binding.tvMeta.setText(e.muscleGroup + " · " + e.difficulty + " · " + e.location);
        binding.tvPrescription.setText(e.sets + " sets x " + e.repetitions
                + " reps\nRest " + e.restSeconds + " seconds between sets");
        binding.tvDescription.setText(e.description);
        binding.btnFavorite.setText(item.isFavorite
                ? "Remove from favorites" : "Add to favorites");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}