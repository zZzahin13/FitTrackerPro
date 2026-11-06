package com.example.fittrackerpro.database;

import com.example.fittrackerpro.database.entity.Meal;

import java.util.ArrayList;
import java.util.List;

/**
 * SAMPLE DATA: approximate values for demonstration, not verified nutrition facts.
 * Real values depend on serving size and how the food is prepared.
 * IDs starting with "food_" mark sample foods; custom foods get random IDs.
 */
public class FoodSeeder {

    public static List<Meal> createFoods() {
        List<Meal> list = new ArrayList<>();
        list.add(food("food_white_rice", "White rice, cooked", "1 cup (160 g)", 205, 4.3, 45, 0.4));
        list.add(food("food_roti", "Roti (whole wheat)", "1 piece (35 g)", 100, 3, 20, 1.5));
        list.add(food("food_paratha", "Paratha", "1 piece (80 g)", 260, 5, 36, 10));
        list.add(food("food_dal", "Dal (lentil curry)", "1 bowl (150 g)", 160, 9, 24, 3));
        list.add(food("food_boiled_egg", "Boiled egg", "1 large (50 g)", 78, 6, 0.6, 5));
        list.add(food("food_omelette", "Omelette (2 eggs)", "1 serving (110 g)", 190, 13, 2, 15));
        list.add(food("food_chicken_curry", "Chicken curry", "1 serving (150 g)", 240, 22, 6, 14));
        list.add(food("food_grilled_chicken", "Grilled chicken breast", "100 g", 165, 31, 0, 3.6));
        list.add(food("food_fish_curry", "Fish curry", "1 piece with gravy (120 g)", 180, 18, 4, 10));
        list.add(food("food_khichuri", "Khichuri", "1 plate (250 g)", 380, 13, 60, 9));
        list.add(food("food_veg_curry", "Mixed vegetable curry", "1 bowl (150 g)", 110, 3, 12, 6));
        list.add(food("food_beef_curry", "Beef curry", "1 serving (150 g)", 300, 25, 5, 20));
        list.add(food("food_banana", "Banana", "1 medium (118 g)", 105, 1.3, 27, 0.4));
        list.add(food("food_apple", "Apple", "1 medium (180 g)", 95, 0.5, 25, 0.3));
        list.add(food("food_orange", "Orange", "1 medium (130 g)", 62, 1.2, 15, 0.2));
        list.add(food("food_mango", "Mango", "1 cup sliced (165 g)", 100, 1.4, 25, 0.6));
        list.add(food("food_milk", "Milk (whole)", "1 cup (240 ml)", 150, 8, 12, 8));
        list.add(food("food_yogurt", "Plain yogurt (doi)", "1 cup (245 g)", 150, 8.5, 11.4, 8));
        list.add(food("food_oatmeal", "Oatmeal, cooked in water", "1 cup (240 g)", 160, 6, 27, 3));
        list.add(food("food_bread", "White bread", "1 slice (28 g)", 75, 2.5, 14, 1));
        list.add(food("food_peanut_butter", "Peanut butter", "1 tablespoon (16 g)", 95, 4, 3, 8));
        list.add(food("food_peanuts", "Roasted peanuts", "30 g", 170, 7, 5, 14));
        list.add(food("food_almonds", "Almonds", "30 g", 175, 6, 6, 15));
        list.add(food("food_chickpeas", "Chickpeas, cooked", "1 cup (165 g)", 270, 14.5, 45, 4));
        list.add(food("food_samosa", "Samosa", "1 piece (60 g)", 160, 3, 18, 8));
        list.add(food("food_muri", "Muri (puffed rice)", "1 bowl (30 g)", 115, 2, 25, 0.3));
        list.add(food("food_milk_tea", "Tea with milk and sugar", "1 cup (200 ml)", 70, 2, 11, 2));
        list.add(food("food_whey", "Whey protein shake (with water)", "1 scoop (30 g)", 120, 24, 3, 1.5));
        list.add(food("food_salad", "Cucumber and tomato salad", "1 bowl (150 g)", 30, 1.5, 6, 0.3));
        list.add(food("food_orange_juice", "Orange juice", "1 glass (250 ml)", 110, 2, 26, 0.5));
        return list;
    }

    private static Meal food(String id, String name, String serving,
                             double kcal, double protein, double carbs, double fat) {
        Meal m = new Meal();
        m.mealId = id;
        m.name = name;
        m.servingSize = serving;
        m.calories = kcal;
        m.protein = protein;
        m.carbohydrates = carbs;
        m.fat = fat;
        return m;
    }
}