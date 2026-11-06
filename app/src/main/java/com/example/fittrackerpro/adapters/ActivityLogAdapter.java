package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.ActivityLog;
import com.example.fittrackerpro.databinding.ItemActivityLogBinding;
import com.example.fittrackerpro.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ActivityLogAdapter extends ListAdapter<ActivityLog, ActivityLogAdapter.ViewHolder> {

    public ActivityLogAdapter() {
        super(DIFF_CALLBACK);
    }

    public ActivityLog getLogAt(int position) {
        return getItem(position);
    }

    private static final DiffUtil.ItemCallback<ActivityLog> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ActivityLog>() {
                @Override
                public boolean areItemsTheSame(@NonNull ActivityLog a, @NonNull ActivityLog b) {
                    return a.activityId.equals(b.activityId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ActivityLog a, @NonNull ActivityLog b) {
                    return Objects.equals(a.activityType, b.activityType)
                            && a.durationMinutes == b.durationMinutes
                            && a.distanceKm == b.distanceKm
                            && a.steps == b.steps
                            && a.caloriesBurned == b.caloriesBurned
                            && a.date == b.date;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActivityLogBinding binding = ItemActivityLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLog log = getItem(position);
        ItemActivityLogBinding b = holder.binding;

        b.tvTitle.setText(log.activityType + " · " + log.durationMinutes + " min");
        b.tvWhen.setText(DateUtil.formatDateTime(log.date));

        List<String> parts = new ArrayList<>();
        if (log.distanceKm > 0) {
            parts.add(String.format(Locale.getDefault(), "%.1f km", log.distanceKm));
        }
        if (log.steps > 0) {
            parts.add(log.steps + " steps");
        }
        b.tvDetails.setText(parts.isEmpty() ? "No distance or steps" : String.join(" · ", parts));
        b.tvKcal.setText("≈ " + Math.round(log.caloriesBurned) + " kcal");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemActivityLogBinding binding;

        ViewHolder(ItemActivityLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}