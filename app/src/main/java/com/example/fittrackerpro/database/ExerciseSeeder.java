package com.example.fittrackerpro.database;

import com.example.fittrackerpro.database.entity.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSeeder {

    // Fixed IDs (not random) so workout plans can refer to these exercises reliably
    public static List<Exercise> createExercises() {
        List<Exercise> list = new ArrayList<>();

        // Chest
        list.add(ex("ex_pushup", "Push-ups", "Chest", "Beginner", "Home", 3, 12, 60,
                "Start in a high plank with hands slightly wider than your shoulders. Keep your body in a straight line, lower your chest to just above the floor, then press back up."));
        list.add(ex("ex_incline_pushup", "Incline Push-ups", "Chest", "Beginner", "Home", 3, 12, 60,
                "Place your hands on a sturdy bench or table. Keep your body straight, lower your chest toward the edge, then push back up. A good step before regular push-ups."));
        list.add(ex("ex_db_bench", "Dumbbell Bench Press", "Chest", "Intermediate", "Gym", 3, 10, 90,
                "Lie on a bench with a dumbbell in each hand at chest level. Press them up until your arms are straight, then lower slowly with control."));
        list.add(ex("ex_bb_bench", "Barbell Bench Press", "Chest", "Advanced", "Gym", 4, 8, 120,
                "Lie on a bench and grip the bar slightly wider than your shoulders. Lower it to your mid-chest, then press up. Use a spotter and a weight you can control."));
        list.add(ex("ex_db_fly", "Dumbbell Fly", "Chest", "Intermediate", "Gym", 3, 12, 60,
                "Lie on a bench holding dumbbells above your chest with soft elbows. Open your arms wide in an arc, feel the stretch, then bring them back together."));

        // Back
        list.add(ex("ex_pullup", "Pull-ups", "Back", "Advanced", "Both", 3, 6, 90,
                "Hang from a bar with an overhand grip. Pull your chest toward the bar by driving your elbows down, then lower slowly. Use a resistance band if it is too hard."));
        list.add(ex("ex_db_row", "Bent-over Dumbbell Row", "Back", "Intermediate", "Both", 3, 10, 75,
                "Hinge forward at the hips with a flat back and a dumbbell in each hand. Pull the weights toward your hips, squeeze your shoulder blades, and lower slowly."));
        list.add(ex("ex_lat_pulldown", "Lat Pulldown", "Back", "Beginner", "Gym", 3, 12, 75,
                "Sit at the machine and hold the bar with a wide grip. Pull it down to your upper chest, keeping your chest up, then let it rise slowly."));
        list.add(ex("ex_superman", "Superman", "Back", "Beginner", "Home", 3, 12, 45,
                "Lie face down with your arms stretched forward. Lift your arms, chest and legs a few centimetres off the floor, hold for a second, then lower."));
        list.add(ex("ex_cable_row", "Seated Cable Row", "Back", "Intermediate", "Gym", 3, 12, 75,
                "Sit with your feet braced and knees slightly bent. Pull the handle to your stomach, squeezing your shoulder blades, then extend your arms slowly."));

        // Legs
        list.add(ex("ex_squat", "Bodyweight Squat", "Legs", "Beginner", "Home", 3, 15, 60,
                "Stand with feet shoulder-width apart. Push your hips back and bend your knees as if sitting in a chair, keep your chest up, then stand tall."));
        list.add(ex("ex_lunge", "Lunges", "Legs", "Beginner", "Home", 3, 10, 60,
                "Step forward and lower until both knees are bent at about 90 degrees. Push back to the start and switch legs. Reps are per leg."));
        list.add(ex("ex_glute_bridge", "Glute Bridge", "Legs", "Beginner", "Home", 3, 15, 45,
                "Lie on your back with knees bent and feet flat. Press through your heels to lift your hips until your body forms a straight line, squeeze, then lower."));
        list.add(ex("ex_bb_squat", "Barbell Squat", "Legs", "Advanced", "Gym", 4, 8, 120,
                "Rest the bar across your upper back. Squat down until your thighs are parallel to the floor with a straight back, then drive up. Learn the form with light weight first."));
        list.add(ex("ex_leg_press", "Leg Press", "Legs", "Beginner", "Gym", 3, 12, 90,
                "Sit in the machine with feet shoulder-width on the platform. Lower the platform until your knees reach about 90 degrees, then press up without locking your knees."));
        list.add(ex("ex_calf_raise", "Calf Raises", "Legs", "Beginner", "Both", 3, 15, 45,
                "Stand tall, rise onto the balls of your feet, pause at the top, and lower slowly. Hold a wall for balance if needed."));

        // Shoulders
        list.add(ex("ex_pike_pushup", "Pike Push-ups", "Shoulders", "Intermediate", "Home", 3, 8, 60,
                "Start in a downward-dog shape with your hips high. Bend your elbows to lower your head toward the floor, then press back up."));
        list.add(ex("ex_db_shoulder_press", "Dumbbell Shoulder Press", "Shoulders", "Intermediate", "Gym", 3, 10, 75,
                "Hold dumbbells at shoulder height. Press them overhead until your arms are straight, then lower slowly. Keep your core tight."));
        list.add(ex("ex_lateral_raise", "Lateral Raises", "Shoulders", "Beginner", "Both", 3, 12, 45,
                "Stand holding light dumbbells at your sides. Raise your arms out to shoulder height with a slight bend in the elbows, then lower slowly."));

        // Arms
        list.add(ex("ex_bicep_curl", "Bicep Curls", "Arms", "Beginner", "Both", 3, 12, 60,
                "Stand with a dumbbell in each hand, palms forward. Curl the weights toward your shoulders without swinging, then lower slowly."));
        list.add(ex("ex_tricep_dip", "Chair Tricep Dips", "Arms", "Intermediate", "Home", 3, 10, 60,
                "Sit on the edge of a sturdy chair with hands beside your hips. Slide forward and bend your elbows to lower your body, then press back up."));
        list.add(ex("ex_hammer_curl", "Hammer Curls", "Arms", "Beginner", "Both", 3, 12, 60,
                "Hold dumbbells with palms facing each other. Curl them up keeping your elbows close to your body, then lower slowly."));
        list.add(ex("ex_tricep_pushdown", "Tricep Pushdown", "Arms", "Beginner", "Gym", 3, 12, 60,
                "Stand at a cable machine with the bar at chest height. Keep your elbows by your sides and push the bar down until your arms are straight, then return slowly."));

        // Core
        list.add(ex("ex_plank", "Plank (30 sec hold)", "Core", "Beginner", "Home", 3, 1, 45,
                "Rest on your forearms and toes with your body in a straight line. Keep your core tight and hold for 30 seconds. Do not let your hips sag."));
        list.add(ex("ex_crunch", "Crunches", "Core", "Beginner", "Home", 3, 15, 45,
                "Lie on your back with knees bent. Curl your shoulders off the floor using your abs, then lower slowly. Do not pull on your neck."));
        list.add(ex("ex_leg_raise", "Leg Raises", "Core", "Intermediate", "Home", 3, 12, 45,
                "Lie flat with your legs straight. Lift them to vertical, then lower slowly without touching the floor. Bend your knees if your lower back arches."));
        list.add(ex("ex_russian_twist", "Russian Twist", "Core", "Intermediate", "Home", 3, 20, 45,
                "Sit with knees bent and lean back slightly. Twist your torso to touch the floor on each side. Reps count both sides."));

        // Full body and cardio
        list.add(ex("ex_burpee", "Burpees", "Full Body", "Intermediate", "Home", 3, 10, 60,
                "From standing, squat and place your hands down, jump your feet back to a plank, return, then jump up. Slow down if your form slips."));
        list.add(ex("ex_jumping_jack", "Jumping Jacks", "Cardio", "Beginner", "Home", 3, 30, 30,
                "Jump while spreading your legs and raising your arms overhead, then jump back together. A good warm-up."));
        list.add(ex("ex_mountain_climber", "Mountain Climbers", "Cardio", "Intermediate", "Home", 3, 30, 45,
                "From a high plank, drive your knees toward your chest one after the other at a steady pace. Reps count both legs."));

        return list;
    }

    private static Exercise ex(String id, String name, String muscle, String difficulty,
                               String location, int sets, int reps, int rest,
                               String description) {
        Exercise e = new Exercise();
        e.exerciseId = id;
        e.name = name;
        e.muscleGroup = muscle;
        e.difficulty = difficulty;
        e.location = location;
        e.sets = sets;
        e.repetitions = reps;
        e.restSeconds = rest;
        e.description = description;
        return e;
    }
}