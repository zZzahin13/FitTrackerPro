package com.example.fittrackerpro.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fittrackerpro.database.entity.PlanExerciseItem;
import com.example.fittrackerpro.databinding.ItemPlanExerciseBinding;
import com.example.fittrackerpro.utils.DateUtil;

public class PlanExerciseAdapter
        extends ListAdapter<PlanExerciseItem, PlanExerciseAdapter.ViewHolder> {

    public interface Listener {
        void onCheckedChanged(PlanExerciseItem item, boolean checked);
        void onItemClick(PlanExerciseItem item);
        void onRestClick(PlanExerciseItem item);
    }

    private final Listener listener;

    public PlanExerciseAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<PlanExerciseItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PlanExerciseItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull PlanExerciseItem a, @NonNull PlanExerciseItem b) {
                    return a.planExerciseId.equals(b.planExerciseId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PlanExerciseItem a, @NonNull PlanExerciseItem b) {
                    return a.completedOn == b.completedOn
                            && a.sets == b.sets
                            && a.repetitions == b.repetitions
                            && a.restSeconds == b.restSeconds;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlanExerciseBinding binding = ItemPlanExerciseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlanExerciseItem item = getItem(position);
        ItemPlanExerciseBinding b = holder.binding;

        b.tvName.setText(item.name);
        b.tvMeta.setText(item.muscleGroup + " · " + item.difficulty);
        b.tvDetails.setText(item.sets + " sets x " + item.repetitions
                + " reps · rest " + item.restSeconds + " s");

        b.cbDone.setOnCheckedChangeListener(null);
        b.cbDone.setChecked(item.completedOn >= DateUtil.startOfWeek());
        b.cbDone.setOnCheckedChangeListener((button, checked) ->
                listener.onCheckedChanged(item, checked));

        b.btnRest.setOnClickListener(v -> listener.onRestClick(item));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlanExerciseBinding binding;

        ViewHolder(ItemPlanExerciseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}