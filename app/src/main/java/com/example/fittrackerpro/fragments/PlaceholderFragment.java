package com.example.fittrackerpro.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fittrackerpro.databinding.FragmentPlaceholderBinding;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_NOTE = "note";

    private FragmentPlaceholderBinding binding;

    public static PlaceholderFragment newInstance(String title, String note) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_NOTE, note);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        Bundle args = getArguments();
        if (args != null) {
            binding.tvTitle.setText(args.getString(ARG_TITLE));
            binding.tvNote.setText(args.getString(ARG_NOTE));
        }
        return binding.getRoot();
    }

    // A fragment's views are destroyed before the fragment itself,
    // so the binding must be cleared to avoid a memory leak.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}