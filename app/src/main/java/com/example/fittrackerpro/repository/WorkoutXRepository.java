package com.example.fittrackerpro.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fittrackerpro.BuildConfig;
import com.example.fittrackerpro.api.WorkoutXClient;
import com.example.fittrackerpro.api.response.WxExercise;
import com.example.fittrackerpro.api.response.WxSearchResponse;
import com.example.fittrackerpro.utils.WorkoutXTerms;

import java.time.Instant;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutXRepository {

    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_KEY = 1;
    public static final int STATUS_NOT_FOUND = 2;
    public static final int STATUS_LIMIT = 3;
    public static final int STATUS_ERROR = 4;

    private static final String TAG = "WorkoutX";
    private static final int STOP_BELOW_REMAINING = 40;       // keep a reserve for the demo
    private static final int LOCAL_REQUEST_CAP = 450;         // used until the server tells us
    private static final long REFRESH_MARGIN_MS = 10 * 60 * 1000L;

    public static class AnimationResult {
        public int status;
        public String exerciseTitle;
        public String model;
        public String mp4Url;
        public boolean watermarked;
        public boolean hasCounterpart;
        public int requestsLeft;
    }

    public interface ResultCallback {
        void onResult(AnimationResult result);
    }

    private final SharedPreferences prefs;

    public WorkoutXRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("workoutx", Context.MODE_PRIVATE);
    }

    public String getPreferredModel() {
        return prefs.getString("preferred_model", null);
    }

    // Shows the animation for one of our exercises
    public void load(String exerciseId, String exerciseName, String preferredModel,
                     ResultCallback cb) {
        if (BuildConfig.WORKOUTX_API_KEY.isEmpty()) {
            cb.onResult(failure(STATUS_NO_KEY));
            return;
        }
        String wxId = prefs.getString("cur_" + exerciseId, null);
        if (wxId != null) {
            loadById(wxId, cb);   // we already know this exercise: no search needed
        } else {
            search(exerciseId, WorkoutXTerms.forExercise(exerciseId, exerciseName),
                    0, preferredModel, cb);
        }
    }

    // Switches between the male and female model of the same movement
    public void switchModel(String exerciseId, ResultCallback cb) {
        String wxId = prefs.getString("cur_" + exerciseId, null);
        String counterpart = wxId == null ? null : prefs.getString("cp_" + wxId, null);
        if (counterpart == null) {
            cb.onResult(failure(STATUS_NOT_FOUND));
            return;
        }
        prefs.edit().putString("cur_" + exerciseId, counterpart).apply();
        loadById(counterpart, result -> {
            if (result.status == STATUS_OK) {
                // New exercises will start with the model the user last chose
                prefs.edit().putString("preferred_model", result.model).apply();
            }
            cb.onResult(result);
        });
    }

    // Uses the saved link if it is still valid, otherwise asks for a fresh one
    private void loadById(String wxId, ResultCallback cb) {
        long expires = prefs.getLong("exp_" + wxId, 0);
        String mp4 = prefs.getString("mp4_" + wxId, null);
        if (mp4 != null && expires - System.currentTimeMillis() > REFRESH_MARGIN_MS) {
            Log.d(TAG, "Saved link used for " + wxId + " (no request spent)");
            cb.onResult(fromCache(wxId));
            return;
        }
        if (blocked()) {
            cb.onResult(failure(STATUS_LIMIT));
            return;
        }

        countRequest();
        WorkoutXClient.get().getExercise(wxId).enqueue(new Callback<WxExercise>() {
            @Override
            public void onResponse(@NonNull Call<WxExercise> call,
                                   @NonNull Response<WxExercise> response) {
                trackQuota(response);
                if (!response.isSuccessful() || response.body() == null) {
                    cb.onResult(failure(statusFor(response.code())));
                    return;
                }
                WxExercise e = response.body();
                if (e.media == null || e.media.mp4 == null) {
                    cb.onResult(failure(STATUS_LIMIT));
                    return;
                }
                save(e);
                cb.onResult(fromCache(e.id));
            }

            @Override
            public void onFailure(@NonNull Call<WxExercise> call, @NonNull Throwable t) {
                Log.w(TAG, "Request failed: " + t.getMessage());
                cb.onResult(failure(STATUS_ERROR));
            }
        });
    }

    // Tries each search word in turn until one finds a match
    private void search(String exerciseId, String[] terms, int index,
                        String preferredModel, ResultCallback cb) {
        if (index >= terms.length) {
            cb.onResult(failure(STATUS_NOT_FOUND));
            return;
        }
        if (blocked()) {
            cb.onResult(failure(STATUS_LIMIT));
            return;
        }

        final String term = terms[index];
        countRequest();
        WorkoutXClient.get().searchByName(term, 10).enqueue(new Callback<WxSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<WxSearchResponse> call,
                                   @NonNull Response<WxSearchResponse> response) {
                trackQuota(response);
                if (!response.isSuccessful() || response.body() == null) {
                    cb.onResult(failure(statusFor(response.code())));
                    return;
                }
                WxExercise best = pickBest(response.body(), term, preferredModel);
                if (best == null) {
                    search(exerciseId, terms, index + 1, preferredModel, cb);
                    return;
                }
                if (best.media == null || best.media.mp4 == null) {
                    cb.onResult(failure(STATUS_LIMIT));
                    return;
                }
                Log.d(TAG, "Matched '" + term + "' -> " + best.name
                        + " (" + best.id + ", " + best.model + ")");
                save(best);
                prefs.edit().putString("cur_" + exerciseId, best.id).apply();
                cb.onResult(fromCache(best.id));
            }

            @Override
            public void onFailure(@NonNull Call<WxSearchResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "Search failed: " + t.getMessage());
                cb.onResult(failure(STATUS_ERROR));
            }
        });
    }

    // Best match = name contains every search word; shortest name wins; preferred model wins ties
    private WxExercise pickBest(WxSearchResponse body, String term, String preferredModel) {
        if (body.results == null) return null;
        String[] words = term.toLowerCase(Locale.ROOT).split("\\s+");
        WxExercise best = null;
        double bestScore = Double.MAX_VALUE;

        for (WxExercise e : body.results) {
            if (e.name == null) continue;
            String name = e.name.toLowerCase(Locale.ROOT);
            boolean allWords = true;
            for (String w : words) {
                if (!name.contains(w)) {
                    allWords = false;
                    break;
                }
            }
            if (!allWords) continue;

            double score = name.length()
                    + (preferredModel != null && preferredModel.equals(e.model) ? 0 : 0.5);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    // Saves the exercise id (kept for good) and the short-lived link (kept until it expires)
    private void save(WxExercise e) {
        long expires;
        try {
            expires = Instant.parse(e.media.expiresAt).toEpochMilli();
        } catch (Exception ex) {
            expires = System.currentTimeMillis() + 60 * 60 * 1000L;
        }
        prefs.edit()
                .putString("mp4_" + e.id, e.media.mp4)
                .putLong("exp_" + e.id, expires)
                .putBoolean("wm_" + e.id, e.media.watermarked)
                .putString("title_" + e.id, e.name)
                .putString("model_" + e.id, e.model)
                .putString("cp_" + e.id, e.counterpartId)
                .apply();
    }

    private AnimationResult fromCache(String wxId) {
        AnimationResult r = new AnimationResult();
        r.status = STATUS_OK;
        r.exerciseTitle = prefs.getString("title_" + wxId, "");
        r.model = prefs.getString("model_" + wxId, "male");
        r.mp4Url = prefs.getString("mp4_" + wxId, null);
        r.watermarked = prefs.getBoolean("wm_" + wxId, true);
        r.hasCounterpart = prefs.getString("cp_" + wxId, null) != null;
        r.requestsLeft = requestsLeft();
        return r;
    }

    private AnimationResult failure(int status) {
        AnimationResult r = new AnimationResult();
        r.status = status;
        r.requestsLeft = requestsLeft();
        return r;
    }

    // ---------- Request budget (the free plan allows 500 requests in total) ----------

    // The server tells us the real number in a response header
    private void trackQuota(Response<?> response) {
        String header = response.headers().get("X-Quota-Remaining");
        if (header == null) return;
        try {
            int remaining = Integer.parseInt(header.trim());
            prefs.edit().putInt("quota_remaining", remaining).apply();
            Log.d(TAG, "Requests left on this key: " + remaining);
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean blocked() {
        int remaining = prefs.getInt("quota_remaining", -1);
        if (remaining >= 0) {
            return remaining < STOP_BELOW_REMAINING;
        }
        return prefs.getInt("requests_made", 0) >= LOCAL_REQUEST_CAP;
    }

    private void countRequest() {
        prefs.edit().putInt("requests_made", prefs.getInt("requests_made", 0) + 1).apply();
    }

    private int requestsLeft() {
        int remaining = prefs.getInt("quota_remaining", -1);
        if (remaining >= 0) return remaining;
        return Math.max(0, 500 - prefs.getInt("requests_made", 0));   // estimate until the first reply
    }

    private int statusFor(int httpCode) {
        switch (httpCode) {
            case 401: return STATUS_NO_KEY;
            case 404: return STATUS_NOT_FOUND;
            case 403:
            case 429: return STATUS_LIMIT;
            default: return STATUS_ERROR;
        }
    }
}