package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.example.fittrackerpro.activities.WorkoutHistoryActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.ExerciseLibraryActivity;
import com.example.fittrackerpro.activities.WorkoutDayActivity;
import com.example.fittrackerpro.adapters.PlanDayAdapter;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.database.entity.WorkoutPlan;
import com.example.fittrackerpro.databinding.FragmentWorkoutBinding;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.viewmodel.WorkoutViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class WorkoutFragment extends Fragment implements PlanDayAdapter.Listener {

    private FragmentWorkoutBinding binding;
    private WorkoutViewModel viewModel;
    private PlanDayAdapter adapter;

    private WorkoutPlan currentPlan;
    private List<DaySummary> currentSummaries = new ArrayList<>();
    private boolean summariesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(WorkoutViewModel.class);

        adapter = new PlanDayAdapter(this);
        binding.rvDays.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDays.setAdapter(adapter);

        viewModel.getPlan().observe(getViewLifecycleOwner(), this::showPlan);
        viewModel.getSummaries().observe(getViewLifecycleOwner(), summaries -> {
            currentSummaries = summaries;
            summariesLoaded = true;
            refreshDays();
        });

        binding.btnCreatePlan.setOnClickListener(v -> showDurationDialog());
        binding.btnNewPlan.setOnClickListener(v -> confirmReplace());
        binding.btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExerciseLibraryActivity.class)));
        binding.btnHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WorkoutHistoryActivity.class)));
        return binding.getRoot();
    }

    private void showPlan(WorkoutPlan plan) {
        currentPlan = plan;
        boolean hasPlan = plan != null;
        binding.layoutNoPlan.setVisibility(hasPlan ? View.GONE : View.VISIBLE);
        binding.layoutPlan.setVisibility(hasPlan ? View.VISIBLE : View.GONE);
        binding.btnNewPlan.setVisibility(hasPlan ? View.VISIBLE : View.GONE);

        if (hasPlan) {
            binding.tvPlanName.setText(plan.planName);
            binding.tvPlanDates.setText(DateUtil.formatDate(plan.startDate)
                    + " to " + DateUtil.formatDate(plan.endDate));
        }
        refreshDays();
    }

    // Builds the 7 weekday rows; days without exercises are rest days
    private void refreshDays() {
        if (!summariesLoaded || currentPlan == null) return;

        List<PlanDayAdapter.DayRow> rows = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            DaySummary found = null;
            for (DaySummary s : currentSummaries) {
                if (s.dayOfWeek == day) found = s;
            }
            rows.add(new PlanDayAdapter.DayRow(day, found));
        }
        adapter.submitList(rows);
    }

    private void confirmReplace() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create a new plan?")
                .setMessage("Your current plan will be replaced. Your workout history is kept.")
                .setPositiveButton("Continue", (d, w) -> showDurationDialog())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDurationDialog() {
        String[] labels = {"1 month", "3 months", "6 months", "1 year"};
        int[] months = {1, 3, 6, 12};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose plan length")
                .setItems(labels, (dialog, which) -> createPlan(months[which]))
                .show();
    }

    private void createPlan(int months) {
        binding.btnCreatePlan.setEnabled(false);
        viewModel.createPlan(months, success -> {
            if (binding == null) return;   // the screen was closed meanwhile
            binding.btnCreatePlan.setEnabled(true);
            if (!success) {
                Toast.makeText(requireContext(),
                        "Please finish your assessment first", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDayClick(PlanDayAdapter.DayRow row) {
        if (row.isRestDay()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(DateUtil.dayName(row.dayOfWeek) + ": rest day")
                    .setMessage(R.string.recovery_tips)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (currentPlan == null) return;

        Intent i = new Intent(requireContext(), WorkoutDayActivity.class);
        i.putExtra(WorkoutDayActivity.EXTRA_PLAN_ID, currentPlan.planId);
        i.putExtra(WorkoutDayActivity.EXTRA_DAY, row.dayOfWeek);
        startActivity(i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}