package com.example.fittrackerpro.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.ExerciseItem;
import com.example.fittrackerpro.databinding.ActivityExerciseDetailBinding;
import com.example.fittrackerpro.repository.WorkoutXRepository;
import com.example.fittrackerpro.viewmodel.AnimationViewModel;
import com.example.fittrackerpro.viewmodel.ExerciseViewModel;

public class ExerciseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXERCISE_ID = "exercise_id";

    private ActivityExerciseDetailBinding binding;
    private ExerciseViewModel viewModel;
    private AnimationViewModel animationViewModel;
    private ExerciseItem current;

    private boolean animationRequested = false;
    private boolean loading = false;
    private boolean preparing = false;
    private String playingUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExerciseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        if (exerciseId == null) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);
        animationViewModel = new ViewModelProvider(this).get(AnimationViewModel.class);

        viewModel.getItem(exerciseId).observe(this, item -> {
            if (item == null) return;
            current = item;
            show(item);

            // Ask for the animation once, when the exercise has loaded
            if (!animationRequested) {
                animationRequested = true;
                animationViewModel.start(item.exercise.exerciseId, item.exercise.name);
            }
        });

        binding.btnFavorite.setOnClickListener(v -> {
            if (current != null) {
                viewModel.toggleFavorite(current);
            }
        });

        setupVideo();

        animationViewModel.getLoading().observe(this, isLoading -> {
            loading = isLoading;
            binding.btnSwitchModel.setEnabled(!isLoading);
            updateProgress();
        });
        animationViewModel.getResult().observe(this, this::showAnimation);
        binding.btnSwitchModel.setOnClickListener(v -> animationViewModel.switchModel());
    }

    private void setupVideo() {
        binding.videoAnim.setOnPreparedListener(mp -> {
            mp.setLooping(true);      // loop forever
            mp.setVolume(0f, 0f);     // muted
            preparing = false;
            updateProgress();
            binding.videoAnim.start();
        });
        binding.videoAnim.setOnErrorListener((mp, what, extra) -> {
            preparing = false;
            updateProgress();
            stopVideo();
            binding.tvAnimMessage.setVisibility(View.VISIBLE);
            binding.tvAnimMessage.setText(
                    "The animation could not be played. It needs an internet connection.");
            return true;
        });
    }

    private void showAnimation(WorkoutXRepository.AnimationResult r) {
        if (r == null) return;

        if (r.status != WorkoutXRepository.STATUS_OK) {
            stopVideo();
            binding.tvAnimMessage.setVisibility(View.VISIBLE);
            binding.tvAnimMessage.setText(messageFor(r.status));
            binding.btnSwitchModel.setVisibility(View.GONE);
            binding.tvAnimInfo.setText("Animation requests left: " + r.requestsLeft);
            return;
        }

        binding.tvAnimMessage.setVisibility(View.GONE);
        binding.tvAnimInfo.setText("Animation: " + r.exerciseTitle + " · " + r.model + " model"
                + (r.watermarked ? " · free preview (watermarked)" : "")
                + "\nSource: WorkoutX · requests left: " + r.requestsLeft);

        binding.btnSwitchModel.setVisibility(r.hasCounterpart ? View.VISIBLE : View.GONE);
        binding.btnSwitchModel.setText("female".equals(r.model)
                ? "Show male model" : "Show female model");

        if (!r.mp4Url.equals(playingUrl)) {
            playingUrl = r.mp4Url;
            preparing = true;
            updateProgress();
            binding.videoAnim.setVisibility(View.VISIBLE);
            binding.videoAnim.setVideoURI(Uri.parse(r.mp4Url));
        }
    }

    private String messageFor(int status) {
        switch (status) {
            case WorkoutXRepository.STATUS_NO_KEY:
                return "No valid WorkoutX key found. Add workoutx.api.key=YOUR_KEY to local.properties, then rebuild the app.";
            case WorkoutXRepository.STATUS_NOT_FOUND:
                return "No animation was found for this exercise.";
            case WorkoutXRepository.STATUS_LIMIT:
                return "The free animation limit has been reached, so this exercise has no animation right now.";
            default:
                return "Could not load the animation. Check your internet connection.";
        }
    }

    private void updateProgress() {
        binding.progressAnim.setVisibility(loading || preparing ? View.VISIBLE : View.GONE);
    }

    private void stopVideo() {
        binding.videoAnim.stopPlayback();
        binding.videoAnim.setVisibility(View.GONE);
        playingUrl = null;
    }

    private void show(ExerciseItem item) {
        Exercise e = item.exercise;
        binding.tvName.setText(e.name);
        binding.tvMeta.setText(e.muscleGroup + " · " + e.difficulty + " · " + e.location);
        binding.tvPrescription.setText(e.sets + " sets x " + e.repetitions
                + " reps\nRest " + e.restSeconds + " seconds between sets");
        binding.tvDescription.setText(e.description);
        binding.btnFavorite.setText(item.isFavorite
                ? "Remove from favorites" : "Add to favorites");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding.videoAnim.isPlaying()) {
            binding.videoAnim.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playingUrl != null && !preparing && !binding.videoAnim.isPlaying()
                && binding.videoAnim.getVisibility() == View.VISIBLE) {
            binding.videoAnim.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.videoAnim.stopPlayback();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}