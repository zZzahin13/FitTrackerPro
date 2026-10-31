package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.databinding.ItemExerciseBinding;

import java.util.Objects;

public class ExerciseAdapter extends ListAdapter<ExerciseItem, ExerciseAdapter.ViewHolder> {

    public interface Listener {
        void onExerciseClick(ExerciseItem item);
        void onFavoriteClick(ExerciseItem item);
    }

    private final Listener listener;

    public ExerciseAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ExerciseItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ExerciseItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ExerciseItem a, @NonNull ExerciseItem b) {
                    return a.exercise.exerciseId.equals(b.exercise.exerciseId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ExerciseItem a, @NonNull ExerciseItem b) {
                    return a.isFavorite == b.isFavorite
                            && Objects.equals(a.exercise.name, b.exercise.name)
                            && Objects.equals(a.exercise.muscleGroup, b.exercise.muscleGroup);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExerciseBinding binding = ItemExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseItem item = getItem(position);
        Exercise e = item.exercise;
        ItemExerciseBinding b = holder.binding;

        b.tvName.setText(e.name);
        b.tvMeta.setText(e.muscleGroup + " · " + e.difficulty + " · " + e.location);
        b.tvDetails.setText(e.sets + " sets x " + e.repetitions + " reps · rest " + e.restSeconds + " s");
        b.btnFavorite.setImageResource(item.isFavorite ? R.drawable.ic_star : R.drawable.ic_star_border);

        holder.itemView.setOnClickListener(v -> listener.onExerciseClick(item));
        b.btnFavorite.setOnClickListener(v -> listener.onFavoriteClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExerciseBinding binding;

        ViewHolder(ItemExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}