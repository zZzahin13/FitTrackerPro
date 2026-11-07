package com.example.fittrackerpro.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for the timed activity session (Day 6, Part 2).
 *
 * The state is kept in SharedPreferences so that it survives screen rotation,
 * leaving the app, and even Android killing the process. The elapsed time is
 * never "counted" - it is worked out from timestamps:
 *
 *   elapsed = accumulatedMs + (running ? now - runningSince : 0)
 *
 * The service and the screens both read this class, and the screens observe
 * it through LiveData, so they always agree.
 */
public class ActivitySessionStore {

    public static final int IDLE = 0;
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;

    private static final String PREFS = "activity_session";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TYPE = "type";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_ACCUMULATED = "accumulated_ms";
    private static final String KEY_RUNNING_SINCE = "running_since";

    /** An immutable snapshot of the session. */
    public static class State {
        public final int status;
        public final String type;
        public final long startedAt;      // wall-clock time the session began
        public final long accumulatedMs;  // time banked before the last pause
        public final long runningSince;   // wall-clock time of the last start/resume (0 when paused)

        State(int status, String type, long startedAt, long accumulatedMs, long runningSince) {
            this.status = status;
            this.type = type;
            this.startedAt = startedAt;
            this.accumulatedMs = accumulatedMs;
            this.runningSince = runningSince;
        }

        public boolean isActive() {
            return status == RUNNING || status == PAUSED;
        }

        public long elapsedMs(long now) {
            if (status == RUNNING) {
                return accumulatedMs + Math.max(0, now - runningSince);
            }
            return accumulatedMs;
        }
    }

    private static final State IDLE_STATE = new State(IDLE, null, 0, 0, 0);
    private static final MutableLiveData<State> live = new MutableLiveData<>();
    private static State current;

    private ActivitySessionStore() {
    }

    public static LiveData<State> observe(Context context) {
        get(context);   // make sure the saved state is loaded first
        return live;
    }

    public static synchronized State get(Context context) {
        if (current == null) {
            SharedPreferences p = prefs(context);
            current = new State(
                    p.getInt(KEY_STATUS, IDLE),
                    p.getString(KEY_TYPE, null),
                    p.getLong(KEY_STARTED_AT, 0),
                    p.getLong(KEY_ACCUMULATED, 0),
                    p.getLong(KEY_RUNNING_SINCE, 0));
            publish();
        }
        return current;
    }

    /** Starts a new session. If one is already active it is kept as it is. */
    public static synchronized State start(Context context, String type) {
        State s = get(context);
        if (s.isActive()) {
            return s;
        }
        long now = System.currentTimeMillis();
        return save(context, new State(RUNNING, type, now, 0, now));
    }

    public static synchronized State pause(Context context) {
        State s = get(context);
        if (s.status != RUNNING) {
            return s;
        }
        long now = System.currentTimeMillis();
        return save(context, new State(PAUSED, s.type, s.startedAt, s.elapsedMs(now), 0));
    }

    public static synchronized State resume(Context context) {
        State s = get(context);
        if (s.status != PAUSED) {
            return s;
        }
        return save(context, new State(RUNNING, s.type, s.startedAt, s.accumulatedMs,
                System.currentTimeMillis()));
    }

    public static synchronized State clear(Context context) {
        prefs(context).edit().clear().apply();
        current = IDLE_STATE;
        publish();
        mirrorToRealtimeDatabase(current);
        return current;
    }

    /** 0:42, 12:05, 1:03:27 */
    public static String formatElapsed(long ms) {
        long totalSeconds = ms / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    private static State save(Context context, State s) {
        prefs(context).edit()
                .putInt(KEY_STATUS, s.status)
                .putString(KEY_TYPE, s.type)
                .putLong(KEY_STARTED_AT, s.startedAt)
                .putLong(KEY_ACCUMULATED, s.accumulatedMs)
                .putLong(KEY_RUNNING_SINCE, s.runningSince)
                .apply();
        current = s;
        publish();
        mirrorToRealtimeDatabase(s);
        return s;
    }

    /**
     * Mirrors live activity session status (IDLE / RUNNING / PAUSED) to Firebase Realtime Database
     * under live_sessions/{uid} for low-latency real-time session tracking.
     */
    private static void mirrorToRealtimeDatabase(State s) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                return;
            }
            String statusLabel = (s.status == RUNNING) ? "RUNNING"
                    : (s.status == PAUSED) ? "PAUSED" : "IDLE";
            Map<String, Object> rtdbMap = new HashMap<>();
            rtdbMap.put("status", statusLabel);
            rtdbMap.put("activityType", s.type != null ? s.type : "None");
            rtdbMap.put("startedAt", s.startedAt);
            rtdbMap.put("accumulatedSeconds", s.accumulatedMs / 1000L);
            rtdbMap.put("updatedAt", System.currentTimeMillis());

            FirebaseDatabase.getInstance()
                    .getReference("live_sessions")
                    .child(user.getUid())
                    .setValue(rtdbMap);
        } catch (Exception ignored) {
            // Non-blocking: local SharedPreferences remains the primary offline source of truth
        }
    }

    private static void publish() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            live.setValue(current);
        } else {
            live.postValue(current);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
