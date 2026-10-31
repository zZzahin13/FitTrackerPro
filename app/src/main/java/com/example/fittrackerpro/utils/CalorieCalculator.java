package com.example.fittrackerpro.utils;

import com.example.fittrackerpro.database.entity.UserProfile;

public class CalorieCalculator {
    // ESTIMATE: general strength training is about 5 METs.
// calories = MET x body weight (kg) x hours
    public static double workoutCalories(double weightKg, int minutes) {
        return 5.0 * weightKg * (minutes / 60.0);
    }

    public static class Targets {
        public double bmr;
        public double tdee;
        public double targetCalories;
        public double proteinG;
        public double carbsG;
        public double fatG;
    }

    // BMR (Mifflin-St Jeor): calories your body burns at complete rest
    public static double bmr(double weightKg, double heightCm, int age, String gender) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        if ("Male".equals(gender)) return base + 5;
        if ("Female".equals(gender)) return base - 161;
        return base - 78;   // average of the two when gender is not specified
    }

    // TDEE = BMR x activity multiplier
    public static double activityMultiplier(String level) {
        if (level == null) return 1.2;
        if (level.startsWith("Lightly")) return 1.375;
        if (level.startsWith("Moderately")) return 1.55;
        if (level.startsWith("Very")) return 1.725;
        return 1.2;   // Sedentary
    }

    // Goal names must match res/values/arrays.xml
    private static double goalFactor(String goal, int age) {
        if (goal == null) return 1.0;
        switch (goal) {
            case "Weight Loss":
                return age < 18 ? 1.0 : 0.80;   // no deficit is suggested under 18
            case "Lean Physique":
                return age < 18 ? 1.0 : 0.90;
            case "Muscle Building":
                return 1.10;
            case "Endurance Improvement":
                return 1.05;
            default:
                return 1.0;   // Weight Maintenance, General Fitness
        }
    }

    // {protein %, carbs %, fat %} of daily calories
    private static double[] macroSplit(String goal) {
        if (goal == null) return new double[]{0.25, 0.45, 0.30};
        switch (goal) {
            case "Weight Loss":
            case "Lean Physique":
                return new double[]{0.30, 0.40, 0.30};
            case "Muscle Building":
                return new double[]{0.30, 0.45, 0.25};
            case "Endurance Improvement":
                return new double[]{0.20, 0.55, 0.25};
            default:
                return new double[]{0.25, 0.45, 0.30};
        }
    }

    public static Targets calculate(UserProfile p) {
        Targets t = new Targets();
        t.bmr = bmr(p.currentWeightKg, p.heightCm, p.age, p.gender);
        t.tdee = t.bmr * activityMultiplier(p.activityLevel);

        // Safety floor: never go below 1200 kcal or your resting BMR
        // (and never above TDEE, for very small people)
        double floor = Math.min(Math.max(1200, t.bmr), t.tdee);
        t.targetCalories = Math.max(t.tdee * goalFactor(p.fitnessGoal, p.age), floor);

        double[] split = macroSplit(p.fitnessGoal);
        t.proteinG = t.targetCalories * split[0] / 4;   // 4 kcal per gram
        t.carbsG = t.targetCalories * split[1] / 4;     // 4 kcal per gram
        t.fatG = t.targetCalories * split[2] / 9;       // 9 kcal per gram
        return t;
    }
}