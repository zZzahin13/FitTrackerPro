package com.example.fittrackerpro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.adapters.ExerciseAdapter;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.databinding.ActivityExerciseLibraryBinding;
import com.example.fittrackerpro.viewmodel.ExerciseViewModel;
import com.google.android.material.chip.Chip;

public class ExerciseLibraryActivity extends AppCompatActivity
        implements ExerciseAdapter.Listener {

    private ActivityExerciseLibraryBinding binding;
    private ExerciseViewModel viewModel;
    private ExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        adapter = new ExerciseAdapter(this);
        binding.rvExercises.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExercises.setAdapter(adapter);

        setupChips();
        setupSearch();

        viewModel.getExercises().observe(this, list -> {
            adapter.submitList(list);
            binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void setupChips() {
        // The ViewModel remembers the filter, so rotation keeps the same chip selected
        binding.chipFavorites.setChecked(viewModel.isFavoritesOnly());

        String[] groups = getResources().getStringArray(R.array.muscle_groups);
        for (String label : groups) {
            Chip chip = (Chip) getLayoutInflater()
                    .inflate(R.layout.item_chip_choice, binding.chipGroup, false);
            chip.setText(label);
            chip.setChecked(label.equals(viewModel.getMuscle()));
            binding.chipGroup.addView(chip);
        }

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            viewModel.setMuscle(chip.getText().toString());
        });

        binding.chipFavorites.setOnCheckedChangeListener((button, checked) ->
                viewModel.setFavoritesOnly(checked));
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onExerciseClick(ExerciseItem item) {
        // Explicit Intent with an extra: tells the next screen which exercise to show
        Intent i = new Intent(this, ExerciseDetailActivity.class);
        i.putExtra(ExerciseDetailActivity.EXTRA_EXERCISE_ID, item.exercise.exerciseId);
        startActivity(i);
    }

    @Override
    public void onFavoriteClick(ExerciseItem item) {
        viewModel.toggleFavorite(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}