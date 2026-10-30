package com.example.fittrackerpro.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.data.Workout;
import com.example.fittrackerpro.databinding.ItemWorkoutBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class WorkoutAdapter extends ListAdapter<Workout, WorkoutAdapter.WorkoutViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Workout workout);
    }

    private OnItemClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());

    public WorkoutAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public Workout getWorkoutAt(int position) {
        return getItem(position);
    }

    // DiffUtil figures out exactly which rows changed, so only those animate
    private static final DiffUtil.ItemCallback<Workout> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Workout>() {
                @Override
                public boolean areItemsTheSame(@NonNull Workout oldItem, @NonNull Workout newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Workout oldItem, @NonNull Workout newItem) {
                    return Objects.equals(oldItem.type, newItem.type)
                            && oldItem.durationMinutes == newItem.durationMinutes
                            && oldItem.calories == newItem.calories
                            && oldItem.distanceKm == newItem.distanceKm
                            && oldItem.dateMillis == newItem.dateMillis;
                }
            };

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkoutBinding binding = ItemWorkoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new WorkoutViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout w = getItem(position);
        ItemWorkoutBinding b = holder.binding;

        b.tvType.setText(w.type);
        b.tvDate.setText(dateFormat.format(new Date(w.dateMillis)));
        b.tvDuration.setText(w.durationMinutes + " min");
        b.tvCalories.setText(w.calories + " kcal");

        if (w.distanceKm > 0) {
            b.tvDistance.setVisibility(View.VISIBLE);
            b.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", w.distanceKm));
        } else {
            b.tvDistance.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(w);
            }
        });
    }

    // The ViewHolder holds the views of one row so they can be reused
    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        final ItemWorkoutBinding binding;

        WorkoutViewHolder(ItemWorkoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}