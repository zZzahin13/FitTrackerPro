package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.WeightHistory;
import com.example.fittrackerpro.databinding.ItemWeightHistoryBinding;
import com.example.fittrackerpro.utils.BMIUtil;
import com.example.fittrackerpro.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightHistoryAdapter extends RecyclerView.Adapter<WeightHistoryAdapter.Holder> {

    private final List<WeightHistory> items = new ArrayList<>();
    private double heightCm = 170;
    private int age = 22;

    public void setUserMetrics(double heightCm, int age) {
        if (heightCm > 0) this.heightCm = heightCm;
        if (age > 0) this.age = age;
        notifyDataSetChanged();
    }

    public void submitList(List<WeightHistory> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public WeightHistory getItemAt(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWeightHistoryBinding b = ItemWeightHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WeightHistory item = items.get(position);
        holder.b.tvWeightValue.setText(String.format(Locale.getDefault(), "%.1f kg", item.weightKg));
        double bmi = BMIUtil.bmi(item.weightKg, heightCm);
        holder.b.tvWeightBmi.setText(String.format(Locale.getDefault(),
                "BMI %.1f • %s", bmi, BMIUtil.category(bmi, age)));
        holder.b.tvWeightDate.setText(DateUtil.formatDate(item.date));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemWeightHistoryBinding b;

        Holder(ItemWeightHistoryBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
