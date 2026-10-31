package com.example.fittrackerpro.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.database.entity.DaySummary;
import com.example.fittrackerpro.databinding.ItemPlanDayBinding;
import com.example.fittrackerpro.utils.DateUtil;

import java.util.Objects;

public class PlanDayAdapter extends ListAdapter<PlanDayAdapter.DayRow, PlanDayAdapter.ViewHolder> {

    // One row per weekday. summary is null on rest days.
    public static class DayRow {
        public final int dayOfWeek;
        public final DaySummary summary;

        public DayRow(int dayOfWeek, DaySummary summary) {
            this.dayOfWeek = dayOfWeek;
            this.summary = summary;
        }

        public boolean isRestDay() {
            return summary == null;
        }
    }

    public interface Listener {
        void onDayClick(DayRow row);
    }

    private final Listener listener;

    public PlanDayAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<DayRow> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DayRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull DayRow a, @NonNull DayRow b) {
                    return a.dayOfWeek == b.dayOfWeek;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DayRow a, @NonNull DayRow b) {
                    if (a.isRestDay() || b.isRestDay()) {
                        return a.isRestDay() == b.isRestDay();
                    }
                    return a.summary.exerciseCount == b.summary.exerciseCount
                            && a.summary.completedCount == b.summary.completedCount
                            && Objects.equals(a.summary.muscles, b.summary.muscles);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlanDayBinding binding = ItemPlanDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayRow row = getItem(position);
        ItemPlanDayBinding b = holder.binding;
        Context context = b.getRoot().getContext();

        b.tvDay.setText(DateUtil.dayName(row.dayOfWeek));

        boolean today = row.dayOfWeek == DateUtil.todayDayOfWeek();
        b.tvToday.setVisibility(today ? android.view.View.VISIBLE : android.view.View.GONE);

        // Highlight today with an accent border
        int strokePx = Math.round(2 * context.getResources().getDisplayMetrics().density);
        b.getRoot().setStrokeWidth(today ? strokePx : 0);
        b.getRoot().setStrokeColor(ContextCompat.getColor(context, R.color.accent));

        if (row.isRestDay()) {
            b.tvFocus.setText("Rest and recovery");
            b.tvStatus.setText("Rest");
        } else {
            b.tvFocus.setText(row.summary.muscles.replace(",", ", "));
            b.tvStatus.setText(row.summary.completedCount + "/" + row.summary.exerciseCount + " done");
        }

        holder.itemView.setOnClickListener(v -> listener.onDayClick(row));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlanDayBinding binding;

        ViewHolder(ItemPlanDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}