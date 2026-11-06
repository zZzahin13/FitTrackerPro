package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.AddFoodActivity;
import com.example.fittrackerpro.adapters.MealLogAdapter;
import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.databinding.FragmentNutritionBinding;
import com.example.fittrackerpro.utils.CalorieCalculator;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.utils.QuantityDialog;
import com.example.fittrackerpro.viewmodel.NutritionViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NutritionFragment extends Fragment implements MealLogAdapter.Listener {

    private static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Evening Snack", "Dinner"};
    private static final int MAX_WATER_PER_DAY_ML = 6000;
    private static final int DEFAULT_WATER_GOAL_ML = 2000;

    private FragmentNutritionBinding binding;
    private NutritionViewModel viewModel;
    private MealLogAdapter adapter;

    private List<FoodLogItem> currentLogs = new ArrayList<>();
    private List<FoodLogItem> weekLogs = new ArrayList<>();
    private CalorieCalculator.Targets targets;   // null until the profile loads
    private int waterGoal = DEFAULT_WATER_GOAL_ML;
    private int waterTotal = 0;
    private int weekWaterTotal = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNutritionBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(NutritionViewModel.class);

        adapter = new MealLogAdapter(this);
        binding.rvMeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMeals.setNestedScrollingEnabled(false);   // the page scrolls, not the list
        binding.rvMeals.setAdapter(adapter);
        setupSwipeToDelete();

        binding.btnPrevDay.setOnClickListener(v -> viewModel.previousDay());
        binding.btnNextDay.setOnClickListener(v -> viewModel.nextDay());
        binding.btnWater250.setOnClickListener(v -> addWater(250));
        binding.btnWater500.setOnClickListener(v -> addWater(500));
        binding.btnWaterUndo.setOnClickListener(v -> viewModel.undoWater());

        viewModel.getSelectedDay().observe(getViewLifecycleOwner(), day -> {
            binding.tvDate.setText(DateUtil.formatDayLabel(day));
            binding.btnNextDay.setEnabled(day < DateUtil.startOfToday());
        });

        viewModel.getLogs().observe(getViewLifecycleOwner(), logs -> {
            currentLogs = logs;
            render();
        });

        viewModel.getWaterTotal().observe(getViewLifecycleOwner(), total -> {
            waterTotal = total != null ? total : 0;
            renderWater();
        });

        viewModel.getWeekLogs().observe(getViewLifecycleOwner(), logs -> {
            weekLogs = logs;
            renderWeek();
        });

        viewModel.getWeekWater().observe(getViewLifecycleOwner(), total -> {
            weekWaterTotal = total != null ? total : 0;
            renderWeek();
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            targets = profile != null ? CalorieCalculator.calculate(profile) : null;
            waterGoal = profile != null
                    ? CalorieCalculator.waterGoalMl(profile.currentWeightKg)
                    : DEFAULT_WATER_GOAL_ML;
            render();
            renderWater();
            renderWeek();
        });

        return binding.getRoot();
    }

    private void addWater(int amountMl) {
        if (waterTotal + amountMl > MAX_WATER_PER_DAY_ML) {
            Toast.makeText(requireContext(),
                    "That is a lot of water for one day. Check the amounts you logged.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        viewModel.addWater(amountMl);
    }

    private void renderWater() {
        if (binding == null) return;
        binding.tvWater.setText(waterTotal + " ml");
        binding.tvWaterGoal.setText("of " + waterGoal + " ml suggested goal (estimate)");
        binding.progressWater.setMax(Math.max(waterGoal, 1));
        binding.progressWater.setProgressCompat(Math.min(waterTotal, waterGoal), true);
        binding.btnWaterUndo.setEnabled(waterTotal > 0);
    }

    // Recalculates the day's totals and rebuilds the four meal sections
    private void render() {
        if (binding == null) return;

        double kcal = 0, protein = 0, carbs = 0, fat = 0;
        for (FoodLogItem l : currentLogs) {
            kcal += l.log.totalCalories;
            protein += l.proteinPerServing * l.log.quantity;
            carbs += l.carbsPerServing * l.log.quantity;
            fat += l.fatPerServing * l.log.quantity;
        }

        binding.tvConsumed.setText(String.valueOf(Math.round(kcal)));

        if (targets != null) {
            int target = (int) Math.round(targets.targetCalories);
            long remaining = target - Math.round(kcal);

            binding.tvTarget.setText("of " + target + " kcal daily target");
            if (remaining >= 0) {
                binding.tvRemaining.setText(remaining + " kcal remaining");
                binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            } else {
                binding.tvRemaining.setText(-remaining + " kcal over target");
                binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
            }
            binding.progressCalories.setMax(Math.max(target, 1));
            binding.progressCalories.setProgressCompat((int) Math.min(Math.round(kcal), target), true);

            binding.tvProtein.setText("Protein\n" + Math.round(protein) + " / " + Math.round(targets.proteinG) + " g");
            binding.tvCarbs.setText("Carbs\n" + Math.round(carbs) + " / " + Math.round(targets.carbsG) + " g");
            binding.tvFat.setText("Fat\n" + Math.round(fat) + " / " + Math.round(targets.fatG) + " g");
        } else {
            binding.tvTarget.setText("Finish your assessment to see a daily target");
            binding.tvRemaining.setText("");
            binding.progressCalories.setProgressCompat(0, false);
            binding.tvProtein.setText("Protein\n" + Math.round(protein) + " g");
            binding.tvCarbs.setText("Carbs\n" + Math.round(carbs) + " g");
            binding.tvFat.setText("Fat\n" + Math.round(fat) + " g");
        }

        // One header per meal, followed by that meal's food lines
        List<MealLogAdapter.Row> rows = new ArrayList<>();
        for (String type : MEAL_TYPES) {
            double mealKcal = 0;
            List<FoodLogItem> items = new ArrayList<>();
            for (FoodLogItem l : currentLogs) {
                if (type.equals(l.log.mealType)) {
                    items.add(l);
                    mealKcal += l.log.totalCalories;
                }
            }
            rows.add(MealLogAdapter.Row.header(type, mealKcal));
            for (FoodLogItem item : items) {
                rows.add(MealLogAdapter.Row.log(item));
            }
        }
        adapter.submitList(rows);
    }

    // Builds the "Last 7 days" card: averages, comparison with the target, and a bar per day
    private void renderWeek() {
        if (binding == null) return;

        long weekStart = viewModel.getWeekStart();
        double[] kcalPerDay = new double[7];
        double protein = 0, carbs = 0, fat = 0;

        for (FoodLogItem l : weekLogs) {
            // Which of the 7 days does this log belong to? (rounding avoids daylight-saving drift)
            int index = (int) Math.round(
                    (DateUtil.dayStartOf(l.log.date) - weekStart) / 86400000.0);
            if (index < 0 || index > 6) continue;
            kcalPerDay[index] += l.log.totalCalories;
            protein += l.proteinPerServing * l.log.quantity;
            carbs += l.carbsPerServing * l.log.quantity;
            fat += l.fatPerServing * l.log.quantity;
        }

        int daysLogged = 0;
        double totalKcal = 0;
        for (double k : kcalPerDay) {
            if (k > 0) {
                daysLogged++;
                totalKcal += k;
            }
        }

        StringBuilder summary = new StringBuilder();
        if (daysLogged == 0) {
            summary.append("No food logged in the last 7 days yet.");
        } else {
            double avg = totalKcal / daysLogged;
            summary.append("Average: ").append(Math.round(avg)).append(" kcal/day (")
                    .append(daysLogged).append(" of 7 days logged)");
            summary.append("\nProtein ").append(Math.round(protein / daysLogged))
                    .append(" g · Carbs ").append(Math.round(carbs / daysLogged))
                    .append(" g · Fat ").append(Math.round(fat / daysLogged)).append(" g per day");

            if (targets != null) {
                long diff = Math.round(avg - targets.targetCalories);
                if (Math.abs(diff) < 50) {
                    summary.append("\nClose to your daily target");
                } else {
                    summary.append("\n").append(Math.abs(diff)).append(" kcal ")
                            .append(diff < 0 ? "below" : "above")
                            .append(" your daily target on average");
                }
            }
            if (daysLogged >= 3 && avg < 1200) {
                summary.append("\nThis is a low intake. If you are not logging everything, "
                        + "add the missing meals. If this is what you really eat, "
                        + "please talk to a doctor or dietitian.");
            }
        }
        summary.append("\nWater: ").append(weekWaterTotal / 7).append(" ml/day average (7 days)");
        binding.tvWeekSummary.setText(summary.toString());

        // One line per day: date, a bar (12 characters wide), and the calories
        double reference = (targets != null && targets.targetCalories > 0)
                ? targets.targetCalories : 2000;
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE dd", Locale.getDefault());
        StringBuilder days = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            days.append(dayFormat.format(new Date(DateUtil.addDays(weekStart, i)))).append("  ");
            int filled = (int) Math.round(Math.min(kcalPerDay[i] / reference, 1.5) * 8);
            for (int b = 0; b < 12; b++) {
                days.append(b < filled ? "█" : "·");
            }
            days.append(String.format(Locale.getDefault(), " %5d", Math.round(kcalPerDay[i])));
            if (i < 6) days.append("\n");
        }
        binding.tvWeekDays.setText(days.toString());
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            // Section headers cannot be swiped
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == MealLogAdapter.TYPE_HEADER) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                MealLogAdapter.Row row = adapter.getRowAt(viewHolder.getBindingAdapterPosition());
                if (row.item == null) return;
                FoodLogItem removed = row.item;
                viewModel.delete(removed.log);

                Snackbar.make(binding.getRoot(), removed.foodName + " removed", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> viewModel.restore(removed.log))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvMeals);
    }

    @Override
    public void onAddClick(String mealType) {
        Intent i = new Intent(requireContext(), AddFoodActivity.class);
        i.putExtra(AddFoodActivity.EXTRA_MEAL_TYPE, mealType);
        i.putExtra(AddFoodActivity.EXTRA_DAY_START, viewModel.getSelectedDayValue());
        startActivity(i);
    }

    @Override
    public void onLogClick(FoodLogItem item) {
        QuantityDialog.show(requireContext(), "Edit servings", item.foodName, item.servingSize,
                item.kcalPerServing, item.log.quantity, "Save",
                quantity -> viewModel.updateQuantity(item, quantity));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}