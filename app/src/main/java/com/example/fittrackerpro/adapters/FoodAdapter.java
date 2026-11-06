package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.databinding.ItemFoodBinding;
import com.example.fittrackerpro.utils.NumberUtil;

import java.util.Objects;

public class FoodAdapter extends ListAdapter<Meal, FoodAdapter.ViewHolder> {

    public interface Listener {
        void onFoodClick(Meal food);
    }

    private final Listener listener;

    public FoodAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Meal> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Meal>() {
                @Override
                public boolean areItemsTheSame(@NonNull Meal a, @NonNull Meal b) {
                    return a.mealId.equals(b.mealId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Meal a, @NonNull Meal b) {
                    return Objects.equals(a.name, b.name) && a.calories == b.calories;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFoodBinding binding = ItemFoodBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meal m = getItem(position);
        ItemFoodBinding b = holder.binding;

        b.tvName.setText(m.name);
        // Sample foods have IDs starting with "food_"; everything else is user-made
        b.tvTag.setText(m.mealId.startsWith("food_") ? "Sample (approx.)" : "Custom");
        b.tvInfo.setText(m.servingSize + " · " + Math.round(m.calories) + " kcal");
        b.tvMacros.setText("Protein " + NumberUtil.trim(m.protein) + " g · Carbs "
                + NumberUtil.trim(m.carbohydrates) + " g · Fat " + NumberUtil.trim(m.fat) + " g");

        holder.itemView.setOnClickListener(v -> listener.onFoodClick(m));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFoodBinding binding;

        ViewHolder(ItemFoodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}