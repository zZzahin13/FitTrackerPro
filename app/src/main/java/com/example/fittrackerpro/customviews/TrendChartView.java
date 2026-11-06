package com.example.fittrackerpro.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Day 7 Custom View: hand-drawn Canvas chart styled after the modern Analytics reference:
 *  - MODE_BAR:  Rounded vertical bars with a Cyan-to-Emerald gradient on dark tracks
 *  - MODE_LINE: Smooth cubic Bezier curve with a glowing Cyan-Emerald gradient fill under the curve
 */
public class TrendChartView extends View {

    public static final int MODE_BAR = 0;
    public static final int MODE_LINE = 1;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path areaPath = new Path();
    private final RectF barRect = new RectF();
    private final RectF trackRect = new RectF();

    private int mode = MODE_BAR;
    private final List<Float> values = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private String emptyMessage = "No data yet";
    private float dp;

    public TrendChartView(Context context) {
        super(context);
        init();
    }

    public TrendChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dp = getResources().getDisplayMetrics().density;

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * dp);
        gridPaint.setColor(Color.parseColor("#1E2836"));

        barTrackPaint.setStyle(Paint.Style.FILL);
        barTrackPaint.setColor(Color.parseColor("#1B2330"));

        barPaint.setStyle(Paint.Style.FILL);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.8f * dp);
        linePaint.setColor(Color.parseColor("#2DD4BF"));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        areaPaint.setStyle(Paint.Style.FILL);

        dotGlowPaint.setStyle(Paint.Style.FILL);
        dotGlowPaint.setColor(Color.parseColor("#4438BDF8"));

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#38BDF8"));

        labelPaint.setColor(Color.parseColor("#94A3B8"));
        labelPaint.setTextSize(10.5f * dp);

        emptyPaint.setColor(Color.parseColor("#94A3B8"));
        emptyPaint.setTextSize(13f * dp);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int mode, List<Float> newValues, List<String> newLabels, String emptyText) {
        this.mode = mode;
        this.values.clear();
        this.labels.clear();
        if (newValues != null) {
            this.values.addAll(newValues);
        }
        if (newLabels != null) {
            this.labels.addAll(newLabels);
        }
        if (emptyText != null) {
            this.emptyMessage = emptyText;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (values.isEmpty()) {
            canvas.drawText(emptyMessage, width / 2f, height / 2f, emptyPaint);
            return;
        }

        float leftPad = 38f * dp;
        float rightPad = 12f * dp;
        float topPad = 14f * dp;
        float bottomPad = 24f * dp;

        float chartW = width - leftPad - rightPad;
        float chartH = height - topPad - bottomPad;
        if (chartW <= 0 || chartH <= 0) {
            return;
        }

        float minVal = Float.MAX_VALUE;
        float maxVal = -Float.MAX_VALUE;
        for (float v : values) {
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
        }

        if (mode == MODE_BAR) {
            minVal = 0f;
            if (maxVal <= 0f) maxVal = 100f;
            else maxVal = maxVal * 1.15f;
        } else {
            if (Math.abs(maxVal - minVal) < 1f) {
                minVal -= 2f;
                maxVal += 2f;
            } else {
                float pad = (maxVal - minVal) * 0.22f;
                minVal = Math.max(0f, minVal - pad);
                maxVal = maxVal + pad;
            }
        }

        // Draw 4 subtle horizontal grid lines & Y-axis labels
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 3; i++) {
            float fraction = i / 3f;
            float y = topPad + chartH * (1f - fraction);
            canvas.drawLine(leftPad, y, leftPad + chartW, y, gridPaint);
            float labelVal = minVal + (maxVal - minVal) * fraction;
            String yText = (mode == MODE_LINE)
                    ? String.format(Locale.getDefault(), "%.1f", labelVal)
                    : String.valueOf(Math.round(labelVal));
            canvas.drawText(yText, leftPad - 6f * dp, y + 3.5f * dp, labelPaint);
        }

        int n = values.size();
        labelPaint.setTextAlign(Paint.Align.CENTER);
        int labelStep = Math.max(1, n / 6);

        if (mode == MODE_BAR) {
            barPaint.setShader(new LinearGradient(
                    0, topPad, 0, topPad + chartH,
                    Color.parseColor("#38BDF8"),
                    Color.parseColor("#10B981"),
                    Shader.TileMode.CLAMP));

            float slotW = chartW / n;
            float barW = Math.min(14f * dp, slotW * 0.52f);
            float corner = barW / 2f;

            for (int i = 0; i < n; i++) {
                float v = Math.max(0f, values.get(i));
                float ratio = (v - minVal) / (maxVal - minVal);
                float barH = Math.max(4f * dp, ratio * chartH);
                float cx = leftPad + slotW * (i + 0.5f);
                float top = topPad + chartH - barH;
                float bottom = topPad + chartH;

                // Full-height dark pill track behind each bar (like reference Analytics chart)
                trackRect.set(cx - barW / 2f, topPad, cx + barW / 2f, bottom);
                canvas.drawRoundRect(trackRect, corner, corner, barTrackPaint);

                barRect.set(cx - barW / 2f, top, cx + barW / 2f, bottom);
                canvas.drawRoundRect(barRect, corner, corner, barPaint);

                if (i < labels.size() && (i % labelStep == 0 || i == n - 1)) {
                    canvas.drawText(labels.get(i), cx, height - 6f * dp, labelPaint);
                }
            }
        } else {
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                float v = values.get(i);
                float ratio = (v - minVal) / (maxVal - minVal);
                xs[i] = (n == 1)
                        ? leftPad + chartW / 2f
                        : leftPad + (chartW * i) / (n - 1f);
                ys[i] = topPad + chartH * (1f - ratio);

                if (i < labels.size() && (i % labelStep == 0 || i == n - 1)) {
                    canvas.drawText(labels.get(i), xs[i], height - 6f * dp, labelPaint);
                }
            }

            linePath.reset();
            areaPath.reset();
            if (n > 1) {
                linePath.moveTo(xs[0], ys[0]);
                areaPath.moveTo(xs[0], topPad + chartH);
                areaPath.lineTo(xs[0], ys[0]);

                for (int i = 0; i < n - 1; i++) {
                    float midX = (xs[i] + xs[i + 1]) / 2f;
                    linePath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1]);
                    areaPath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1]);
                }
                areaPath.lineTo(xs[n - 1], topPad + chartH);
                areaPath.close();

                areaPaint.setShader(new LinearGradient(
                        0, topPad, 0, topPad + chartH,
                        Color.parseColor("#662DD4BF"),
                        Color.parseColor("#002DD4BF"),
                        Shader.TileMode.CLAMP));
                canvas.drawPath(areaPath, areaPaint);

                linePaint.setShader(new LinearGradient(
                        leftPad, 0, leftPad + chartW, 0,
                        Color.parseColor("#38BDF8"),
                        Color.parseColor("#34D399"),
                        Shader.TileMode.CLAMP));
                canvas.drawPath(linePath, linePaint);
            }

            for (int i = 0; i < n; i++) {
                canvas.drawCircle(xs[i], ys[i], 6.5f * dp, dotGlowPaint);
                canvas.drawCircle(xs[i], ys[i], 3.5f * dp, dotPaint);
            }
        }
    }
}
