package com.example.pickleball.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pickleball.R;

public class RadarView extends View {

    private Paint circlePaint1, circlePaint2, circlePaint3;
    private Paint sweepPaint;
    private Paint centerDotPaint;
    private Paint dotPaint;

    private float sweepAngle = 0f;
    private ValueAnimator animator;

    private final float[] dotAngles  = {30f, 110f, 200f, 280f};
    private final float[] dotRadii   = {0.55f, 0.35f, 0.7f, 0.45f};

    public RadarView(Context context) {
        super(context);
        init(context);
    }

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int green = ContextCompat.getColor(context, R.color.green_primary);
        int greenLight = ContextCompat.getColor(context, R.color.green_light);

        circlePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint1.setStyle(Paint.Style.STROKE);
        circlePaint1.setColor(green);
        circlePaint1.setAlpha(60);
        circlePaint1.setStrokeWidth(1.5f);

        circlePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint2.setStyle(Paint.Style.STROKE);
        circlePaint2.setColor(green);
        circlePaint2.setAlpha(40);
        circlePaint2.setStrokeWidth(1.5f);

        circlePaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint3.setStyle(Paint.Style.STROKE);
        circlePaint3.setColor(green);
        circlePaint3.setAlpha(20);
        circlePaint3.setStrokeWidth(1.5f);

        sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sweepPaint.setStyle(Paint.Style.FILL);

        centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(green);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(green);
        dotPaint.setAlpha(180);

        startAnimation();
    }

    private void startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(anim -> {
            sweepAngle = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float maxR = Math.min(cx, cy) - 4f;

        // Draw 3 concentric rings
        canvas.drawCircle(cx, cy, maxR, circlePaint3);
        canvas.drawCircle(cx, cy, maxR * 0.66f, circlePaint2);
        canvas.drawCircle(cx, cy, maxR * 0.33f, circlePaint1);

        // Draw sweep gradient arc
        int greenColor = ContextCompat.getColor(getContext(), R.color.green_primary);
        SweepGradient gradient = new SweepGradient(cx, cy,
                new int[]{0x0034C759, 0x6034C759, greenColor, 0x0034C759},
                new float[]{0f, 0.5f, 0.99f, 1f});
        sweepPaint.setShader(gradient);

        canvas.save();
        canvas.rotate(sweepAngle, cx, cy);
        RectF oval = new RectF(cx - maxR, cy - maxR, cx + maxR, cy + maxR);
        canvas.drawArc(oval, 0f, 360f, true, sweepPaint);
        canvas.restore();

        // Draw outer ring fill again to clip the sweep inside circle
        Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clipPaint.setStyle(Paint.Style.STROKE);
        clipPaint.setColor(0xFF1D1D1F);
        clipPaint.setStrokeWidth(6f);
        canvas.drawCircle(cx, cy, maxR + 3f, clipPaint);

        // Draw nearby player dots
        for (int i = 0; i < dotAngles.length; i++) {
            float angleDeg = dotAngles[i] + sweepAngle * 0.1f;
            float rad = (float) Math.toRadians(angleDeg);
            float r = maxR * dotRadii[i];
            float dx = cx + r * (float) Math.cos(rad);
            float dy = cy + r * (float) Math.sin(rad);
            canvas.drawCircle(dx, dy, 6f, dotPaint);
        }

        // Center dot (current user)
        canvas.drawCircle(cx, cy, 10f, centerDotPaint);
        Paint innerDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerDot.setStyle(Paint.Style.FILL);
        innerDot.setColor(0xFF1D1D1F);
        canvas.drawCircle(cx, cy, 5f, innerDot);
    }

    public void stopAnimation() {
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
