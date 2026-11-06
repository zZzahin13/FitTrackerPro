package com.example.fittrackerpro.utils;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.example.fittrackerpro.databinding.DialogActivityEntryBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ActivityEntryDialog {

    public interface Callback {
        // dayStart is 0 in finish mode (the caller already knows the start time)
        void onSave(String type, long dayStart, int minutes, double km, int steps);
    }

    public static void showManual(Context context, double weightKg, double heightCm,
                                  Callback callback) {
        show(context, true, null, 0, weightKg, heightCm, null, callback);
    }

    public static void showFinish(Context context, String type, int minutes,
                                  double weightKg, double heightCm,
                                  Runnable onDiscard, Callback callback) {
        show(context, false, type, minutes, weightKg, heightCm, onDiscard, callback);
    }

    private static void show(Context context, boolean manual, String fixedType, int fixedMinutes,
                             double weightKg, double heightCm,
                             Runnable onDiscard, Callback callback) {
        DialogActivityEntryBinding b =
                DialogActivityEntryBinding.inflate(LayoutInflater.from(context));
        b.groupManual.setVisibility(manual ? View.VISIBLE : View.GONE);
        b.tvIntro.setVisibility(manual ? View.GONE : View.VISIBLE);
        if (!manual) {
            b.tvIntro.setText(fixedType + " · " + fixedMinutes + " min");
        }

        // Live preview of the estimated calories and steps
        Runnable updatePreview = () -> {
            String type = manual ? b.spType.getSelectedItem().toString() : fixedType;
            int minutes = manual ? parseInt(text(b.etDuration)) : fixedMinutes;
            if (minutes < 1) {
                b.tvPreview.setText("");
                return;
            }
            double km = number(text(b.etDistance));
            StringBuilder sb = new StringBuilder("≈ ")
                    .append(Math.round(CalorieCalculator.activityCalories(type, weightKg, minutes)))
                    .append(" kcal burned (estimate)");
            if (text(b.etSteps).isEmpty() && !Double.isNaN(km) && km > 0) {
                int estimated = CalorieCalculator.estimateSteps(type, km, heightCm);
                if (estimated > 0) {
                    sb.append("\n≈ ").append(estimated).append(" steps (estimate from distance)");
                }
            }
            b.tvPreview.setText(sb.toString());
        };
        updatePreview.run();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview.run();
            }
        };
        b.etDuration.addTextChangedListener(watcher);
        b.etDistance.addTextChangedListener(watcher);
        b.etSteps.addTextChangedListener(watcher);
        b.spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePreview.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(manual ? "Log an activity" : "Save your session")
                .setView(b.getRoot())
                .setPositiveButton("Save", null)   // set below so errors keep the dialog open
                .setNegativeButton(manual ? "Cancel" : "Discard", (d, w) -> {
                    if (onDiscard != null) {
                        onDiscard.run();
                    }
                })
                .create();
        if (!manual) {
            dialog.setCancelable(false);   // the user must save or discard
        }

        dialog.setOnShowListener(di ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    b.tilDuration.setError(null);
                    b.tilDistance.setError(null);
                    b.tilSteps.setError(null);

                    String type = manual ? b.spType.getSelectedItem().toString() : fixedType;
                    int minutes = manual ? parseInt(text(b.etDuration)) : fixedMinutes;
                    double km = number(text(b.etDistance));
                    double steps = number(text(b.etSteps));

                    if (manual && (minutes < 1 || minutes > 1440)) {
                        b.tilDuration.setError("Enter 1 to 1440 minutes");
                        return;
                    }
                    if (Double.isNaN(km) || km < 0 || km > 300) {
                        b.tilDistance.setError("Enter 0 to 300 km");
                        return;
                    }
                    // Reject impossible speeds (for example 20 km walked in 10 minutes)
                    if (km > 0 && km / (minutes / 60.0) > CalorieCalculator.maxSpeedKmh(type)) {
                        b.tilDistance.setError("That distance looks too far for "
                                + minutes + " minutes");
                        return;
                    }
                    if (Double.isNaN(steps) || steps < 0 || steps > 200000) {
                        b.tilSteps.setError("Enter 0 to 200000");
                        return;
                    }

                    long dayStart = manual
                            ? DateUtil.addDays(DateUtil.startOfToday(),
                            -b.spWhen.getSelectedItemPosition())
                            : 0;
                    dialog.dismiss();
                    callback.onSave(type, dayStart, minutes, km, (int) Math.round(steps));
                }));
        dialog.show();
    }

    private static String text(EditText view) {
        return view.getText().toString().trim();
    }

    // Empty = -1 (not entered)
    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
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