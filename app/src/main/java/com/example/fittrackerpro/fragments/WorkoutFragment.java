package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fittrackerpro.activities.ExerciseLibraryActivity;
import com.example.fittrackerpro.databinding.FragmentWorkoutBinding;

public class WorkoutFragment extends Fragment {

    private FragmentWorkoutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);

        binding.btnLibrary.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ExerciseLibraryActivity.class)));

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}