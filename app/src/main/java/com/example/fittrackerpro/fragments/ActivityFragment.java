package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.activities.TrackingActivity;
import com.example.fittrackerpro.adapters.ActivityLogAdapter;
import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.databinding.FragmentActivityBinding;
import com.example.fittrackerpro.utils.ActivityEntryDialog;
import com.example.fittrackerpro.utils.ActivitySessionStore;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.viewmodel.ActivityViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {

    private static final int DAILY_GOAL_MINUTES = 30;

    private FragmentActivityBinding binding;
    private ActivityViewModel viewModel;
    private ActivityLogAdapter adapter;

    private double weightKg = 70;     // replaced by the profile values once they load
    private double heightCm = 170;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ActivityViewModel.class);

        adapter = new ActivityLogAdapter();
        binding.rvActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvActivities.setAdapter(adapter);
        setupSwipeToDelete();

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                weightKg = profile.currentWeightKg;
                heightCm = profile.heightCm;
            }
        });

        viewModel.getLogs().observe(getViewLifecycleOwner(), logs -> {
            adapter.submitList(logs);
            binding.tvEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
            renderToday(logs);
        });

        ActivitySessionStore.observe(requireContext()).observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.isActive()) {
                binding.btnStartSession.setText("Session active (" + state.type + ")");
            } else {
                binding.btnStartSession.setText("Start timed session");
            }
        });

        binding.btnStartSession.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TrackingActivity.class)));

        binding.btnLogManual.setOnClickListener(v ->
                ActivityEntryDialog.showManual(requireContext(), weightKg, heightCm,
                        (type, dayStart, minutes, km, steps) ->
                                viewModel.addLog(type, DateUtil.logTimeFor(dayStart),
                                        minutes, km, steps, null)));

        return binding.getRoot();
    }

    // Adds up today's entries
    private void renderToday(List<ActivityLog> logs) {
        long today = DateUtil.startOfToday();
        int minutes = 0;
        int steps = 0;
        double km = 0;
        double kcal = 0;
        for (ActivityLog l : logs) {
            if (l.date >= today) {
                minutes += l.durationMinutes;
                steps += l.steps;
                km += l.distanceKm;
                kcal += l.caloriesBurned;
            }
        }

        binding.tvActiveMinutes.setText(minutes + " min active today");
        binding.tvGoal.setText("Daily goal: " + DAILY_GOAL_MINUTES + " minutes");
        binding.progressActive.setMax(DAILY_GOAL_MINUTES);
        binding.progressActive.setProgressCompat(Math.min(minutes, DAILY_GOAL_MINUTES), true);
        binding.tvTotals.setText(String.format(Locale.getDefault(),
                "%.1f km • %d steps • about %d kcal (estimates)", km, steps, Math.round(kcal)));
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
                ActivityLog removed = adapter.getLogAt(viewHolder.getBindingAdapterPosition());
                viewModel.delete(removed);

                Snackbar.make(binding.getRoot(), "Activity removed", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> viewModel.restore(removed))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvActivities);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
