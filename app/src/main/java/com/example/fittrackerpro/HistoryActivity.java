package com.example.fittrackerpro;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.adapter.WorkoutAdapter;
import com.example.fittrackerpro.data.AppDatabase;
import com.example.fittrackerpro.data.Workout;
import com.example.fittrackerpro.data.WorkoutDao;
import com.example.fittrackerpro.databinding.ActivityHistoryBinding;
import com.example.fittrackerpro.databinding.DialogAddWorkoutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private WorkoutDao dao;
    private WorkoutAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dao = AppDatabase.getInstance(this).workoutDao();

        adapter = new WorkoutAdapter();
        binding.rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvWorkouts.setAdapter(adapter);

        adapter.setOnItemClickListener(w -> Toast.makeText(this,
                w.type + ": " + w.durationMinutes + " min, " + w.calories + " kcal",
                Toast.LENGTH_SHORT).show());

        // The list updates itself whenever the database changes
        dao.getAllWorkouts().observe(this, workouts -> {
            adapter.submitList(workouts);
            binding.tvEmpty.setVisibility(workouts.isEmpty() ? View.VISIBLE : View.GONE);
        });

        binding.fabAdd.setOnClickListener(v -> showAddDialog());
        setupSwipeToDelete();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
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
                Workout deleted = adapter.getWorkoutAt(viewHolder.getBindingAdapterPosition());
                AppDatabase.dbExecutor.execute(() -> dao.delete(deleted));

                Snackbar.make(binding.getRoot(), "Workout deleted", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.fabAdd)
                        .setAction("UNDO", v ->
                                AppDatabase.dbExecutor.execute(() -> dao.insert(deleted)))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvWorkouts);
    }

    private void showAddDialog() {
        DialogAddWorkoutBinding d = DialogAddWorkoutBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Add workout")
                .setView(d.getRoot())
                .setPositiveButton("Save", null)   // set below so errors keep the dialog open
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String type = d.spType.getSelectedItem().toString();
                    int duration = parseInt(d.etDuration.getText().toString().trim());
                    int calories = parseInt(d.etCalories.getText().toString().trim());
                    double distance = parseDouble(d.etDistance.getText().toString().trim());

                    d.tilDuration.setError(null);
                    if (duration <= 0) {
                        d.tilDuration.setError("Enter minutes (1 or more)");
                        return;
                    }
                    if (calories < 0 || distance < 0) {
                        Toast.makeText(this, "Check calories and distance",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Workout workout = new Workout(type, duration, calories,
                            distance, System.currentTimeMillis());
                    AppDatabase.dbExecutor.execute(() -> dao.insert(workout));
                    dialog.dismiss();
                }));

        dialog.show();
    }

    // Empty text counts as 0, bad text counts as -1
    private int parseInt(String s) {
        if (s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parseDouble(String s) {
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}