package com.example.fittrackerpro.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WorkoutXTerms {

    private static final Map<String, String[]> TERMS = new HashMap<>();

    static {
        TERMS.put("ex_pushup", new String[]{"push up"});
        TERMS.put("ex_incline_pushup", new String[]{"incline push up"});
        TERMS.put("ex_db_bench", new String[]{"dumbbell bench press"});
        TERMS.put("ex_bb_bench", new String[]{"barbell bench press"});
        TERMS.put("ex_db_fly", new String[]{"dumbbell fly", "dumbbell flye"});
        TERMS.put("ex_pullup", new String[]{"pull up"});
        TERMS.put("ex_db_row", new String[]{"bent over row", "dumbbell row"});
        TERMS.put("ex_lat_pulldown", new String[]{"lat pulldown", "pulldown"});
        TERMS.put("ex_superman", new String[]{"superman"});
        TERMS.put("ex_cable_row", new String[]{"seated row", "cable row"});
        TERMS.put("ex_squat", new String[]{"bodyweight squat", "squat"});
        TERMS.put("ex_lunge", new String[]{"lunge"});
        TERMS.put("ex_glute_bridge", new String[]{"glute bridge"});
        TERMS.put("ex_bb_squat", new String[]{"barbell squat", "barbell full squat"});
        TERMS.put("ex_leg_press", new String[]{"leg press"});
        TERMS.put("ex_calf_raise", new String[]{"calf raise"});
        TERMS.put("ex_pike_pushup", new String[]{"pike push up"});
        TERMS.put("ex_db_shoulder_press", new String[]{"dumbbell shoulder press"});
        TERMS.put("ex_lateral_raise", new String[]{"lateral raise"});
        TERMS.put("ex_bicep_curl", new String[]{"bicep curl"});
        TERMS.put("ex_tricep_dip", new String[]{"bench dip", "tricep dip"});
        TERMS.put("ex_hammer_curl", new String[]{"hammer curl"});
        TERMS.put("ex_tricep_pushdown", new String[]{"pushdown"});
        TERMS.put("ex_plank", new String[]{"plank"});
        TERMS.put("ex_crunch", new String[]{"crunch"});
        TERMS.put("ex_leg_raise", new String[]{"leg raise"});
        TERMS.put("ex_russian_twist", new String[]{"russian twist"});
        TERMS.put("ex_burpee", new String[]{"burpee"});
        TERMS.put("ex_jumping_jack", new String[]{"jumping jack"});
        TERMS.put("ex_mountain_climber", new String[]{"mountain climber"});
    }

    public static String[] forExercise(String exerciseId, String name) {
        String[] terms = TERMS.get(exerciseId);
        if (terms != null) {
            return terms;
        }
        // Exercises added later: use the cleaned-up name ("Plank (30 sec hold)" -> "plank")
        String cleaned = name.toLowerCase(Locale.ROOT)
                .replaceAll("\\(.*?\\)", " ")
                .replace("-", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return new String[]{cleaned};
    }
}