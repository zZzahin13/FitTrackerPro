package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.WorkoutSession;
import com.example.fittrackerpro.databinding.ItemWorkoutSessionBinding;
import com.example.fittrackerpro.utils.DateUtil;

public class WorkoutSessionAdapter
        extends ListAdapter<WorkoutSession, WorkoutSessionAdapter.ViewHolder> {

    public WorkoutSessionAdapter() {
        super(DIFF_CALLBACK);
    }

    public WorkoutSession getSessionAt(int position) {
        return getItem(position);
    }

    private static final DiffUtil.ItemCallback<WorkoutSession> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WorkoutSession>() {
                @Override
                public boolean areItemsTheSame(@NonNull WorkoutSession a, @NonNull WorkoutSession b) {
                    return a.sessionId.equals(b.sessionId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull WorkoutSession a, @NonNull WorkoutSession b) {
                    return a.date == b.date
                            && a.durationMinutes == b.durationMinutes
                            && a.caloriesBurned == b.caloriesBurned
                            && a.completed == b.completed;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutSessionBinding binding = ItemWorkoutSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutSession s = getItem(position);
        ItemWorkoutSessionBinding b = holder.binding;

        b.tvDate.setText(DateUtil.formatDateTime(s.date));
        b.tvDuration.setText(s.durationMinutes + " min");
        b.tvCalories.setText("About " + Math.round(s.caloriesBurned) + " kcal (estimate)");
        b.tvStatus.setText(s.completed ? "Completed" : "Partial");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWorkoutSessionBinding binding;

        ViewHolder(ItemWorkoutSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}