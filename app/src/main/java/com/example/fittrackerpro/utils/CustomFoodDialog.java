package com.example.fittrackerpro.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.fittrackerpro.database.entity.Meal;
import com.example.fittrackerpro.databinding.DialogCustomFoodBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomFoodDialog {

    public interface Callback {
        void onCreated(Meal meal);
    }

    public static void show(Context context, Callback callback) {
        DialogCustomFoodBinding b = DialogCustomFoodBinding.inflate(LayoutInflater.from(context));
        b.etServing.setText("1 serving");

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Create custom food")
                .setView(b.getRoot())
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    b.tilName.setError(null);
                    b.tilServing.setError(null);
                    b.tilCalories.setError(null);
                    b.tilProtein.setError(null);
                    b.tilCarbs.setError(null);
                    b.tilFat.setError(null);

                    String name = text(b.etName);
                    String serving = text(b.etServing);
                    String kcalText = text(b.etCalories);
                    double kcal = number(kcalText);
                    double protein = number(text(b.etProtein));
                    double carbs = number(text(b.etCarbs));
                    double fat = number(text(b.etFat));

                    if (name.isEmpty()) {
                        b.tilName.setError("Enter a name");
                        return;
                    }
                    if (serving.isEmpty()) {
                        b.tilServing.setError("Enter a serving size");
                        return;
                    }
                    if (kcalText.isEmpty() || Double.isNaN(kcal) || kcal < 0 || kcal > 5000) {
                        b.tilCalories.setError("Enter 0 to 5000");
                        return;
                    }
                    if (Double.isNaN(protein) || protein < 0 || protein > 500) {
                        b.tilProtein.setError("Enter 0 to 500");
                        return;
                    }
                    if (Double.isNaN(carbs) || carbs < 0 || carbs > 500) {
                        b.tilCarbs.setError("Enter 0 to 500");
                        return;
                    }
                    if (Double.isNaN(fat) || fat < 0 || fat > 500) {
                        b.tilFat.setError("Enter 0 to 500");
                        return;
                    }

                    Meal meal = new Meal();      // gets a random ID (not "food_...")
                    meal.name = name;
                    meal.servingSize = serving;
                    meal.calories = kcal;
                    meal.protein = protein;
                    meal.carbohydrates = carbs;
                    meal.fat = fat;

                    dialog.dismiss();
                    callback.onCreated(meal);
                }));
        dialog.show();
    }

    private static String text(EditText view) {
        return view.getText().toString().trim();
    }

    // Empty = 0, not a number = NaN
    private static double number(String s) {
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}