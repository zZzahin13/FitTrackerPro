package com.example.fittrackerpro.utils;

import com.example.fittrackerpro.database.entity.Exercise;
import com.example.fittrackerpro.database.entity.PlanExercise;
import com.example.fittrackerpro.database.entity.UserProfile;
import com.example.fittrackerpro.database.entity.WorkoutPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rule-based plan generator. Rules:
 * 1. Days per week decides the split (full body, upper/lower, push/pull/legs).
 * 2. Location and experience decide which exercises are allowed.
 * 3. Workout length decides how many exercises per day.
 * 4. Goal adjusts sets, rest time and adds a cardio finisher.
 * 5. Limitations (keywords) exclude risky exercises.
 * Because it is a plain class with one method, an AI service could replace it later.
 */
public class PlanGenerator {

    public static class Result {
        public WorkoutPlan plan;
        public final List<PlanExercise> items = new ArrayList<>();
    }

    private static final String[] FULL_BODY = {"Legs", "Chest", "Back", "Shoulders", "Core", "Arms"};
    private static final String[] UPPER = {"Chest", "Back", "Shoulders", "Arms", "Core"};
    private static final String[] LOWER = {"Legs", "Core", "Legs", "Cardio"};
    private static final String[] PUSH = {"Chest", "Shoulders", "Arms", "Chest"};
    private static final String[] PULL = {"Back", "Arms", "Back", "Core"};
    private static final String[] LEGS_DAY = {"Legs", "Core", "Legs", "Legs"};

    public static Result generate(UserProfile profile, List<Exercise> catalog, int months) {
        Result result = new Result();
        result.plan = newPlan(profile, months);

        int daysPerWeek = Math.max(2, Math.min(6, profile.workoutDaysPerWeek));
        int[] days = workoutDays(daysPerWeek);
        String[][] templates = split(daysPerWeek, profile.experience);
        int perDay = Math.max(3, Math.min(8, profile.workoutMinutes / 10 + 1));
        Set<String> excluded = excludedFor(profile.limitations);
        Map<String, Integer> rotation = new HashMap<>();

        for (int d = 0; d < days.length; d++) {
            String[] groups = templates[d];
            Set<String> usedToday = new HashSet<>();
            int attempts = 0;
            int g = 0;

            while (usedToday.size() < perDay && attempts < 40) {
                attempts++;
                String muscle = groups[g % groups.length];
                g++;
                Exercise pick = nextExercise(catalog, muscle, profile, excluded, rotation);
                if (pick == null || !usedToday.add(pick.exerciseId)) continue;
                result.items.add(toPlanExercise(result.plan.planId, days[d], pick, profile));
            }

            if (needsCardio(profile.fitnessGoal)) {
                Exercise cardio = nextExercise(catalog, "Cardio", profile, excluded, rotation);
                if (cardio != null && usedToday.add(cardio.exerciseId)) {
                    result.items.add(toPlanExercise(result.plan.planId, days[d], cardio, profile));
                }
            }
        }
        return result;
    }

    private static WorkoutPlan newPlan(UserProfile profile, int months) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.userId = profile.userId;
        String length = months == 12 ? "1 year" : months + (months == 1 ? " month" : " months");
        plan.planName = profile.fitnessGoal + " · " + length;
        plan.durationMonths = months;
        plan.fitnessGoal = profile.fitnessGoal;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(DateUtil.startOfToday());
        plan.startDate = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, months);
        plan.endDate = cal.getTimeInMillis();

        plan.isSynced = false;
        plan.updatedAt = System.currentTimeMillis();
        return plan;
    }

    // 1 = Monday ... 7 = Sunday
    private static int[] workoutDays(int daysPerWeek) {
        switch (daysPerWeek) {
            case 2: return new int[]{1, 4};
            case 3: return new int[]{1, 3, 5};
            case 4: return new int[]{1, 2, 4, 5};
            case 5: return new int[]{1, 2, 3, 5, 6};
            default: return new int[]{1, 2, 3, 4, 5, 6};
        }
    }

    private static String[][] split(int daysPerWeek, String experience) {
        switch (daysPerWeek) {
            case 2:
                return new String[][]{FULL_BODY, FULL_BODY};
            case 3:
                return "Beginner".equals(experience)
                        ? new String[][]{FULL_BODY, FULL_BODY, FULL_BODY}
                        : new String[][]{PUSH, PULL, LEGS_DAY};
            case 4:
                return new String[][]{UPPER, LOWER, UPPER, LOWER};
            case 5:
                return new String[][]{PUSH, PULL, LEGS_DAY, UPPER, LOWER};
            default:
                return new String[][]{PUSH, PULL, LEGS_DAY, PUSH, PULL, LEGS_DAY};
        }
    }

    // Picks the next allowed exercise for a muscle group, rotating so repeated days vary
    private static Exercise nextExercise(List<Exercise> catalog, String muscle, UserProfile p,
                                         Set<String> excluded, Map<String, Integer> rotation) {
        List<Exercise> strict = new ArrayList<>();
        List<Exercise> relaxed = new ArrayList<>();

        for (Exercise e : catalog) {
            if (!muscle.equals(e.muscleGroup) || excluded.contains(e.exerciseId)) continue;
            // Home users cannot use gym-only equipment (gym users can do anything)
            if ("Home".equals(p.workoutLocation) && "Gym".equals(e.location)) continue;
            relaxed.add(e);
            if (rank(e.difficulty) <= rank(p.experience)) strict.add(e);
        }

        List<Exercise> list = strict.isEmpty() ? relaxed : strict;
        if (list.isEmpty()) return null;

        int index = rotation.containsKey(muscle) ? rotation.get(muscle) : 0;
        rotation.put(muscle, index + 1);
        return list.get(index % list.size());
    }

    private static int rank(String level) {
        if ("Advanced".equals(level)) return 3;
        if ("Intermediate".equals(level)) return 2;
        return 1;
    }

    private static boolean needsCardio(String goal) {
        return "Weight Loss".equals(goal) || "Lean Physique".equals(goal)
                || "Endurance Improvement".equals(goal);
    }

    private static PlanExercise toPlanExercise(String planId, int day, Exercise e, UserProfile p) {
        PlanExercise pe = new PlanExercise();
        pe.planId = planId;
        pe.exerciseId = e.exerciseId;
        pe.dayOfWeek = day;

        int sets = e.sets;
        if ("Beginner".equals(p.experience)) sets = Math.min(sets, 3);
        if ("Muscle Building".equals(p.fitnessGoal) && !"Beginner".equals(p.experience)) {
            sets = Math.min(sets + 1, 5);
        }

        int rest = e.restSeconds;
        if (needsCardio(p.fitnessGoal)) rest = Math.max(30, rest - 15);
        if ("Muscle Building".equals(p.fitnessGoal)) rest = rest + 15;

        pe.sets = sets;
        pe.repetitions = e.repetitions;
        pe.restSeconds = rest;
        return pe;
    }

    // Very simple keyword rules. They reduce risk but are NOT medical advice.
    private static Set<String> excludedFor(String limitations) {
        Set<String> out = new HashSet<>();
        if (limitations == null) return out;
        String t = limitations.toLowerCase(Locale.ROOT);

        if (t.contains("knee")) {
            out.addAll(Arrays.asList("ex_lunge", "ex_bb_squat", "ex_squat", "ex_leg_press",
                    "ex_burpee", "ex_jumping_jack", "ex_mountain_climber"));
        }
        if (t.contains("back")) {
            out.addAll(Arrays.asList("ex_bb_squat", "ex_db_row", "ex_superman",
                    "ex_leg_raise", "ex_russian_twist", "ex_burpee"));
        }
        if (t.contains("shoulder")) {
            out.addAll(Arrays.asList("ex_pike_pushup", "ex_db_shoulder_press", "ex_lateral_raise",
                    "ex_pullup", "ex_db_fly", "ex_bb_bench"));
        }
        if (t.contains("wrist")) {
            out.addAll(Arrays.asList("ex_pushup", "ex_incline_pushup", "ex_pike_pushup",
                    "ex_plank", "ex_burpee", "ex_mountain_climber", "ex_tricep_dip"));
        }
        return out;
    }
}