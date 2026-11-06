package com.example.fittrackerpro.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.example.fittrackerpro.databinding.DialogQuantityBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class QuantityDialog {

    public interface Callback {
        void onConfirm(double quantity);
    }

    public static void show(Context context, String title, String foodName, String servingSize,
                            double kcalPerServing, double initialQuantity,
                            String positiveLabel, Callback callback) {
        DialogQuantityBinding b = DialogQuantityBinding.inflate(LayoutInflater.from(context));
        b.tvFoodInfo.setText(foodName + "\n1 serving = " + servingSize + " · "
                + Math.round(kcalPerServing) + " kcal");
        b.etQuantity.setText(NumberUtil.trim(initialQuantity));
        updatePreview(b, kcalPerServing);

        // Live preview: the calories update while you type
        b.etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview(b, kcalPerServing);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(b.getRoot())
                .setPositiveButton(positiveLabel, null)   // set below so errors keep it open
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(di ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    double q = parse(b.etQuantity.getText().toString().trim());
                    if (Double.isNaN(q) || q < 0.25 || q > 20) {
                        b.tilQuantity.setError("Enter between 0.25 and 20 servings");
                        return;
                    }
                    dialog.dismiss();
                    callback.onConfirm(q);
                }));
        dialog.show();
    }

    private static void updatePreview(DialogQuantityBinding b, double kcalPerServing) {
        double q = parse(b.etQuantity.getText().toString().trim());
        b.tilQuantity.setError(null);
        b.tvPreview.setText(Double.isNaN(q) ? "" : "= " + Math.round(q * kcalPerServing) + " kcal");
    }

    private static double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}