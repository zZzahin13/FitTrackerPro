package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.FoodLogItem;
import com.example.fittrackerpro.databinding.ItemFoodLogBinding;
import com.example.fittrackerpro.databinding.ItemMealHeaderBinding;
import com.example.fittrackerpro.utils.NumberUtil;

public class MealLogAdapter extends ListAdapter<MealLogAdapter.Row, RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_LOG = 1;

    // One row of the list: either a meal header or a food line
    public static class Row {
        public final int type;
        public final String mealType;
        public final double kcal;
        public final FoodLogItem item;

        private Row(int type, String mealType, double kcal, FoodLogItem item) {
            this.type = type;
            this.mealType = mealType;
            this.kcal = kcal;
            this.item = item;
        }

        public static Row header(String mealType, double kcal) {
            return new Row(TYPE_HEADER, mealType, kcal, null);
        }

        public static Row log(FoodLogItem item) {
            return new Row(TYPE_LOG, null, 0, item);
        }

        String key() {
            return type == TYPE_HEADER ? "h_" + mealType : "l_" + item.log.logId;
        }
    }

    public interface Listener {
        void onAddClick(String mealType);
        void onLogClick(FoodLogItem item);
    }

    private final Listener listener;

    public MealLogAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public Row getRowAt(int position) {
        return getItem(position);
    }

    private static final DiffUtil.ItemCallback<Row> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Row>() {
                @Override
                public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                    return a.key().equals(b.key());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    if (a.type == TYPE_HEADER) {
                        return a.kcal == b.kcal;
                    }
                    return a.item.log.quantity == b.item.log.quantity
                            && a.item.log.totalCalories == b.item.log.totalCalories;
                }
            };

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemMealHeaderBinding.inflate(inflater, parent, false));
        }
        return new LogHolder(ItemFoodLogBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);

        if (holder instanceof HeaderHolder) {
            ItemMealHeaderBinding b = ((HeaderHolder) holder).binding;
            b.tvMealTitle.setText(row.mealType);
            b.tvMealKcal.setText(Math.round(row.kcal) + " kcal");
            b.btnAdd.setOnClickListener(v -> listener.onAddClick(row.mealType));
        } else {
            ItemFoodLogBinding b = ((LogHolder) holder).binding;
            FoodLogItem item = row.item;
            double q = item.log.quantity;

            b.tvFoodName.setText(item.foodName);
            b.tvFoodDetails.setText(NumberUtil.trim(q) + " x " + item.servingSize
                    + "  ·  P " + Math.round(item.proteinPerServing * q)
                    + "  C " + Math.round(item.carbsPerServing * q)
                    + "  F " + Math.round(item.fatPerServing * q));
            b.tvKcal.setText(Math.round(item.log.totalCalories) + " kcal");

            holder.itemView.setOnClickListener(v -> listener.onLogClick(item));
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemMealHeaderBinding binding;

        HeaderHolder(ItemMealHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class LogHolder extends RecyclerView.ViewHolder {
        final ItemFoodLogBinding binding;

        LogHolder(ItemFoodLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}