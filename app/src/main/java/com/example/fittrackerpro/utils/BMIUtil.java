package com.example.fittrackerpro.utils;

public class BMIUtil {

    // BMI = weight (kg) / height (m) squared
    public static double bmi(double weightKg, double heightCm) {
        double meters = heightCm / 100.0;
        return weightKg / (meters * meters);
    }

    // WHO adult categories. They do not apply to people under 18.
    public static String category(double bmi, int age) {
        if (age < 18) return "Adult categories do not apply under 18";
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Healthy weight";
        if (bmi < 30) return "Overweight";
        return "Obesity";
    }
}