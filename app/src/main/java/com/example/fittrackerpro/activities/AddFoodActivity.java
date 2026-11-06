package com.example.fittrackerpro.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fittrackerpro.adapters.FoodAdapter;
import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.databinding.ActivityAddFoodBinding;
import com.example.fittrackerpro.utils.CustomFoodDialog;
import com.example.fittrackerpro.utils.DateUtil;
import com.example.fittrackerpro.utils.QuantityDialog;
import com.example.fittrackerpro.viewmodel.AddFoodViewModel;

public class AddFoodActivity extends AppCompatActivity implements FoodAdapter.Listener {

    public static final String EXTRA_MEAL_TYPE = "meal_type";
    public static final String EXTRA_DAY_START = "day_start";

    private ActivityAddFoodBinding binding;
    private AddFoodViewModel viewModel;
    private String mealType;
    private long dayStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mealType = getIntent().getStringExtra(EXTRA_MEAL_TYPE);
        dayStart = getIntent().getLongExtra(EXTRA_DAY_START, DateUtil.startOfToday());
        if (mealType == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add to " + mealType);
        }

        viewModel = new ViewModelProvider(this).get(AddFoodViewModel.class);

        FoodAdapter adapter = new FoodAdapter(this);
        binding.rvFoods.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFoods.setAdapter(adapter);

        viewModel.getFoods().observe(this, foods -> {
            adapter.submitList(foods);
            binding.tvEmpty.setVisibility(foods.isEmpty() ? View.VISIBLE : View.GONE);
        });

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

        binding.btnCustom.setOnClickListener(v ->
                CustomFoodDialog.show(this, meal -> {
                    viewModel.addCustomFood(meal);
                    Toast.makeText(this, meal.name + " created. Search for it and tap it to add.",
                            Toast.LENGTH_LONG).show();
                }));
    }

    @Override
    public void onFoodClick(Meal food) {
        QuantityDialog.show(this, "Add to " + mealType, food.name, food.servingSize,
                food.calories, 1, "Add", quantity -> {
                    viewModel.addLog(food, quantity, mealType, dayStart);
                    Toast.makeText(this, food.name + " added", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}