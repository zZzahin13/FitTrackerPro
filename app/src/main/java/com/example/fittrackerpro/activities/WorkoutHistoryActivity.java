package com.example.fittrackerpro.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.adapters.WorkoutSessionAdapter;
import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.databinding.ActivityWorkoutHistoryBinding;
import com.example.fittrackerpro.viewmodel.WorkoutHistoryViewModel;
import com.google.android.material.snackbar.Snackbar;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private ActivityWorkoutHistoryBinding binding;
    private WorkoutHistoryViewModel viewModel;
    private WorkoutSessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkoutHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(WorkoutHistoryViewModel.class);

        adapter = new WorkoutSessionAdapter();
        binding.rvSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSessions.setAdapter(adapter);

        viewModel.getSessions().observe(this, sessions -> {
            adapter.submitList(sessions);
            binding.tvEmpty.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);

            double totalKcal = 0;
            for (WorkoutSession s : sessions) {
                totalKcal += s.caloriesBurned;
            }
            binding.tvSummary.setText(sessions.size() + " workouts · about "
                    + Math.round(totalKcal) + " kcal in total (estimate)");
        });

        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                WorkoutSession deleted =
                        adapter.getSessionAt(viewHolder.getBindingAdapterPosition());
                viewModel.delete(deleted);

                Snackbar.make(binding.getRoot(), "Workout deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> viewModel.restore(deleted))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvSessions);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}