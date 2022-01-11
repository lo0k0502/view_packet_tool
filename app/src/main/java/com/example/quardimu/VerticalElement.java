package com.example.quardimu;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;

public class VerticalElement {


    public static class VerticalSeekBar extends androidx.appcompat.widget.AppCompatSeekBar {

        public VerticalSeekBar(Context context) {
            super(context);
        }

        public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public VerticalSeekBar(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(h, w, oldh, oldw);
        }

        @Override
        public synchronized void setProgress(int progress)  // it is necessary for calling setProgress on click of a button
        {
            super.setProgress(progress);
            onSizeChanged(getWidth(), getHeight(), 0, 0);
        }
        @Override
        protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        }

        protected void onDraw(Canvas c) {
            c.rotate(-90);
            c.translate(-getHeight(), 0);

            super.onDraw(c);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    break;
            }
            return true;
        }
    }




    public static class VerticalButton extends androidx.appcompat.widget.AppCompatButton {
        final boolean topDown;

        public VerticalButton(Context context,
                              AttributeSet attrs) {
            super(context, attrs);
            final int gravity = getGravity();
            if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
                setGravity((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
                topDown = false;
            } else {
                topDown = true;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            TextPaint textPaint = getPaint();
            textPaint.setColor(getCurrentTextColor());
            textPaint.drawableState = getDrawableState();
            canvas.save();
            if (topDown) {
                canvas.translate(getWidth(), 0);
                canvas.rotate(90);
            } else {
                canvas.translate(0, getHeight());
                canvas.rotate(-90);
            }

            canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
            getLayout().draw(canvas);
            canvas.restore();
        }
    }
}
