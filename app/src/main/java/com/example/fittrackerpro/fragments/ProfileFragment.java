package com.example.fittrackerpro.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.work.WorkInfo;

import com.example.fittrackerpro.R;
import com.example.fittrackerpro.activities.AssessmentActivity;
import com.example.fittrackerpro.activities.AssessmentResultActivity;
import com.example.fittrackerpro.activities.LoginActivity;
import com.example.fittrackerpro.activities.ProgressActivity;
import com.example.fittrackerpro.activities.SettingsActivity;
import com.example.fittrackerpro.database.FitTrackerDatabase;
import com.example.fittrackerpro.databinding.FragmentProfileBinding;
import com.example.fittrackerpro.utils.ProfilePhotoHelper;
import com.example.fittrackerpro.utils.SyncManager;
import com.example.fittrackerpro.workers.SyncWorker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/**
 * Day 8B: Profile tab featuring:
 *  1. Android Photo Picker (ActivityResultContracts.PickVisualMedia) + ContentResolver copy to internal storage
 *  2. Cloud Firestore Sync card with pending unsynced badge & live WorkManager status
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    private int unsyncedProfile = 0;
    private int unsyncedActivities = 0;
    private int unsyncedSessions = 0;
    private int unsyncedWeights = 0;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null || getContext() == null) {
                    return;
                }
                FitTrackerDatabase.dbExecutor.execute(() -> {
                    boolean ok = ProfilePhotoHelper.copyUriToInternalStorage(requireContext(), uri);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (ok) {
                                renderAvatar();
                                Toast.makeText(requireContext(),
                                        "Profile photo saved to private storage",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Could not load selected photo",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = "";
        if (user != null) {
            uid = user.getUid();
            String name = user.getDisplayName();
            binding.tvName.setText(name != null && !name.isEmpty() ? name : "Athlete");
            binding.tvEmail.setText(user.getEmail());
        }

        renderAvatar();
        binding.tvSyncStatus.setText(SyncManager.getLastSyncSummary(requireContext()));

        View.OnClickListener pickPhotoClick = v -> pickPhotoLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
        binding.cardProfileAvatar.setOnClickListener(pickPhotoClick);
        binding.btnChangePhoto.setOnClickListener(pickPhotoClick);

        // Observe unsynced counts from Room to display live pending badge
        if (!uid.isEmpty()) {
            FitTrackerDatabase db = FitTrackerDatabase.getInstance(requireContext());
            db.userProfileDao().observeProfile(uid).observe(getViewLifecycleOwner(), profile -> {
                if (profile != null && profile.name != null && !profile.name.isEmpty()) {
                    binding.tvName.setText(profile.name);
                }
                unsyncedProfile = (profile != null && !profile.isSynced) ? 1 : 0;
                updateUnsyncedBadge();
            });
            db.activityLogDao().observeUnsyncedCount(uid).observe(getViewLifecycleOwner(), c -> {
                unsyncedActivities = c != null ? c : 0;
                updateUnsyncedBadge();
            });
            db.workoutSessionDao().observeUnsyncedCount(uid).observe(getViewLifecycleOwner(), c -> {
                unsyncedSessions = c != null ? c : 0;
                updateUnsyncedBadge();
            });
            db.weightHistoryDao().observeUnsyncedCount(uid).observe(getViewLifecycleOwner(), c -> {
                unsyncedWeights = c != null ? c : 0;
                updateUnsyncedBadge();
            });
        }

        // Observe WorkManager Firestore sync status
        SyncManager.observeSyncWork(requireContext()).observe(getViewLifecycleOwner(), workInfos -> {
            if (binding == null || workInfos == null || workInfos.isEmpty()) {
                return;
            }
            WorkInfo info = workInfos.get(0);
            WorkInfo.State state = info.getState();
            if (state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                binding.btnSyncNow.setEnabled(false);
                binding.btnSyncNow.setText("Syncing to Firestore...");
                binding.tvSyncStatus.setText("Uploading unsynced records (requires internet)...");
            } else {
                binding.btnSyncNow.setEnabled(true);
                binding.btnSyncNow.setText("Sync to Cloud Now");
                binding.tvSyncStatus.setText(SyncManager.getLastSyncSummary(requireContext()));
                if (state == WorkInfo.State.FAILED) {
                    String err = info.getOutputData().getString(SyncWorker.KEY_ERROR_MESSAGE);
                    if (err != null && !err.isEmpty()) {
                        binding.tvSyncStatus.setText("Sync error: " + err);
                    }
                }
            }
        });

        binding.btnSyncNow.setOnClickListener(v -> {
            SyncManager.triggerSyncNow(requireContext());
            Toast.makeText(requireContext(),
                    "Cloud sync started via WorkManager",
                    Toast.LENGTH_SHORT).show();
        });

        binding.btnProgress.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProgressActivity.class)));
        binding.btnTargets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AssessmentResultActivity.class)));
        binding.btnEditAssessment.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AssessmentActivity.class)));
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
        return binding.getRoot();
    }

    private void updateUnsyncedBadge() {
        if (binding == null) return;
        int totalPending = unsyncedProfile + unsyncedActivities + unsyncedSessions + unsyncedWeights;
        if (totalPending == 0) {
            binding.tvUnsyncedBadge.setText("All synced");
        } else {
            binding.tvUnsyncedBadge.setText(String.format(Locale.getDefault(),
                    "%d pending", totalPending));
        }
    }

    private void renderAvatar() {
        if (binding == null || getContext() == null) return;
        Bitmap bmp = ProfilePhotoHelper.loadAvatarBitmap(requireContext());
        if (bmp != null) {
            binding.ivProfileAvatar.setPadding(0, 0, 0, 0);
            ImageViewCompat.setImageTintList(binding.ivProfileAvatar, null);
            binding.ivProfileAvatar.setImageBitmap(bmp);
        } else {
            int pad = (int) (20 * getResources().getDisplayMetrics().density);
            binding.ivProfileAvatar.setPadding(pad, pad, pad, pad);
            binding.ivProfileAvatar.setImageResource(R.drawable.ic_nav_profile);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
