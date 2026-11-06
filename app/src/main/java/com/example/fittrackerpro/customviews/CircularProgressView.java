package com.example.fittrackerpro.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Day 7 Custom View: draws a modern fitness ring on Canvas.
 * Supports:
 *  - Custom accent colors (Orange for Calories, Cyan for Steps, Emerald for Active Time)
 *  - Concentric dual-ring mode (Cyan + Emerald outer/inner arcs for Nutrition Tracker)
 *  - Adaptive typography for both compact 3-ring rows and large hero rings
 */
public class CircularProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF oval = new RectF();
    private final RectF innerOval = new RectF();

    private float current = 0f;
    private float target = 2000f;
    private String centerValue = "0";
    private String centerLabel = "";

    private int customColor = Color.parseColor("#2DD4BF");
    private boolean useCustomColor = false;
    private boolean dualRingMode = false;
    private float secondaryFraction = 0.65f;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(Color.parseColor("#222B38"));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setColor(customColor);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        innerTrackPaint.setStyle(Paint.Style.STROKE);
        innerTrackPaint.setColor(Color.parseColor("#1B2430"));
        innerTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        innerArcPaint.setStyle(Paint.Style.STROKE);
        innerArcPaint.setColor(Color.parseColor("#38BDF8"));
        innerArcPaint.setStrokeCap(Paint.Cap.ROUND);

        valuePaint.setColor(Color.parseColor("#F8FAFC"));
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(Color.parseColor("#94A3B8"));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setRingColor(int color) {
        this.customColor = color;
        this.useCustomColor = true;
        this.arcPaint.setShader(null);
        this.arcPaint.setColor(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        this.trackPaint.setColor(Color.argb(45, r, g, b));
        invalidate();
    }

    public void setDualRing(boolean enabled, float innerRatio) {
        this.dualRingMode = enabled;
        this.secondaryFraction = Math.max(0f, Math.min(1f, innerRatio));
        invalidate();
    }

    public void setProgress(float current, float target, String centerValue, String centerLabel) {
        this.current = Math.max(0f, current);
        this.target = target > 0f ? target : 1f;
        this.centerValue = centerValue != null ? centerValue : "";
        this.centerLabel = centerLabel != null ? centerLabel : "";

        if (!useCustomColor) {
            if (this.current > this.target * 1.1f) {
                arcPaint.setShader(null);
                arcPaint.setColor(Color.parseColor("#F59E0B"));
            } else {
                arcPaint.setColor(Color.parseColor("#2DD4BF"));
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        if (size <= 0) return;

        // Scale stroke and typography proportionally to the view size
        boolean compact = size < (int) (110 * getResources().getDisplayMetrics().density);
        float stroke = compact ? size * 0.11f : size * 0.085f;
        trackPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);

        float pad = stroke * 1.15f;
        float left = (w - size) / 2f + pad;
        float top = (h - size) / 2f + pad;
        float right = (w + size) / 2f - pad;
        float bottom = (h + size) / 2f - pad;
        oval.set(left, top, right, bottom);

        float cx = w / 2f;
        float cy = h / 2f;

        if (dualRingMode && !useCustomColor && current <= target * 1.1f) {
            SweepGradient sweep = new SweepGradient(cx, cy,
                    new int[]{
                            Color.parseColor("#38BDF8"),
                            Color.parseColor("#2DD4BF"),
                            Color.parseColor("#34D399"),
                            Color.parseColor("#38BDF8")
                    }, null);
            arcPaint.setShader(sweep);
        }

        canvas.drawOval(oval, trackPaint);

        float fraction = Math.min(1f, current / target);
        canvas.drawArc(oval, -90f, Math.max(4f, fraction * 360f), false, arcPaint);

        if (dualRingMode) {
            float innerStroke = stroke * 0.62f;
            innerTrackPaint.setStrokeWidth(innerStroke);
            innerArcPaint.setStrokeWidth(innerStroke);
            float inset = stroke * 1.15f;
            innerOval.set(oval.left + inset, oval.top + inset, oval.right - inset, oval.bottom - inset);
            canvas.drawOval(innerOval, innerTrackPaint);
            canvas.drawArc(innerOval, -90f, Math.max(6f, secondaryFraction * 360f), false, innerArcPaint);
        }

        boolean hasLabel = centerLabel != null && !centerLabel.isEmpty();
        valuePaint.setTextSize(compact ? size * 0.20f : size * 0.19f);
        labelPaint.setTextSize(compact ? size * 0.105f : size * 0.078f);

        if (hasLabel) {
            canvas.drawText(centerValue, cx, cy + valuePaint.getTextSize() * 0.12f, valuePaint);
            canvas.drawText(centerLabel, cx, cy + valuePaint.getTextSize() * 0.12f + labelPaint.getTextSize() * 1.35f, labelPaint);
        } else {
            Paint.FontMetrics fm = valuePaint.getFontMetrics();
            float textOffset = (fm.descent - fm.ascent) / 2f - fm.descent;
            canvas.drawText(centerValue, cx, cy + textOffset, valuePaint);
        }
    }
}
