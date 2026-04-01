package com.example.mealmate.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

public class StrikethroughTextView extends AppCompatTextView {

    private float strikethroughProgress = 0f;
    private Paint strikethroughPaint;
    private ValueAnimator strikethroughAnimator;

    public StrikethroughTextView(Context context) {
        super(context);
        init();
    }

    public StrikethroughTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StrikethroughTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strikethroughPaint = new Paint();
        strikethroughPaint.setStyle(Paint.Style.STROKE);
        strikethroughPaint.setColor(getCurrentTextColor());
        strikethroughPaint.setStrokeWidth(getTextSize() / 14);

        strikethroughAnimator = ValueAnimator.ofFloat(0f, 1f);
        strikethroughAnimator.setDuration(500);
        strikethroughAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        strikethroughAnimator.addUpdateListener(animation -> {
            strikethroughProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (strikethroughProgress > 0) {
            float width = getWidth() * strikethroughProgress;
            float startX = 0;
            float stopX = startX + width;
            float y = getHeight() / 2f;

            canvas.drawLine(startX, y, stopX, y, strikethroughPaint);
        }
    }

    public boolean isStrikethrough() {
        return strikethroughProgress >= 1f;
    }

    public void setStrikethrough(boolean strikethrough) {
        if (strikethrough) {
            if (strikethroughProgress < 1f) {
                strikethroughAnimator.start();
            }
        } else {
            strikethroughAnimator.cancel();
            strikethroughProgress = 0f;
            invalidate();
        }
    }
}