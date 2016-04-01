package com.abraxascreative.tipandsplit.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Scroller;
import android.widget.TextView;

import com.abraxascreative.tipandsplit.R;

public class RemotelyScrollableInteger extends TextView {
    private int initialValue;
    private int minValue;
    private int maxValue;
    private boolean doesRoundRobin;
    private String formatString;

    private static final float SCROLL_FACTOR = 0.006f;  // These two values were determined through user testing
    private static final float FLING_FACTOR = 0.05f;

    private float currentFloatValue;  // Keeps track of scroll position, which maps to integer via Math.round

    private Scroller scroller;
    private ValueAnimator valueAnimator;

    private OnIntValueChangeListener onIntValueChangeListener;

    public RemotelyScrollableInteger(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RemotelyScrollableInteger, 0, 0);

        try {
            initialValue = a.getInt(R.styleable.RemotelyScrollableInteger_initialValue, 0);
            minValue = a.getInt(R.styleable.RemotelyScrollableInteger_minValue, 0);
            maxValue = a.getInt(R.styleable.RemotelyScrollableInteger_maxValue, 0);
            doesRoundRobin = a.getBoolean(R.styleable.RemotelyScrollableInteger_doesRoundRobin, false);

            formatString = a.getString(R.styleable.RemotelyScrollableInteger_formatString);  // null if undefined
            formatString = formatString == null ? "%d" : formatString;  // Set default if attribute was not defined
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        // Make view focusable
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        // Initialize the float value using the initialValue integer
        setCurrentFloatValue((float) initialValue, true);  // Force update/redraw, in case initialValue == 0

        scroller = new Scroller(getContext());

        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!scroller.isFinished()) {
                    scroller.computeScrollOffset();
                    setIntValue(scroller.getCurrY());
                } else {
                    valueAnimator.cancel();
                    snapToInt();
                }
            }
        });
    }



    public interface OnIntValueChangeListener {
        void onIntValueChange(int newValue);
    }

    public void setOnIntValueChangeListener(OnIntValueChangeListener onIntValueChangeListener) {
        this.onIntValueChangeListener = onIntValueChangeListener;
    }

    public void setIntValue(int value) {
        setCurrentFloatValue((float) value);
    }

    public int getCurrentIntValue() {
        return Math.round(currentFloatValue);
    }

    private String getDisplayText() {
        return String.format(formatString, getCurrentIntValue());
    }

    private void updateText() {
        setText(getDisplayText());
    }

    public void snapToInt() {
        setCurrentFloatValue(getCurrentIntValue());
    }

    private void setCurrentFloatValue(float newValue) {
        setCurrentFloatValue(newValue, false);
    }

    private void setCurrentFloatValue(float newValue, boolean forceUpdate) {
        // Respect the min and max of the value by either wrapping the value back around (if it should round robin)
        // or holding the value at the minimum or maximum
        if (newValue > maxValue) {
            newValue = doesRoundRobin ? newValue - maxValue : maxValue;
        } else if (newValue < minValue) {
            newValue = doesRoundRobin ? newValue + maxValue : minValue;
        }
        // Update the current value, but only redraw if the rounded value has changed (to prevent unnecessary redraws)
        int roundedDiff = Math.abs(Math.round(currentFloatValue) - Math.round(newValue));
        currentFloatValue = newValue;
        if (roundedDiff >= 1 || forceUpdate) {
            snapToInt();  // Prevents number from changing over half the scrolling distance if user reverses direction
            updateText();
            // Also call the listener, if one was set
            if (onIntValueChangeListener != null) {
                onIntValueChangeListener.onIntValueChange(getCurrentIntValue());
            }
        }
    }

    public void scroll(float distanceY) {
        float scaledDistanceY = SCROLL_FACTOR * distanceY;
        float newValue = currentFloatValue + scaledDistanceY;
        setCurrentFloatValue(newValue);
    }

    public void fling(float velocityY) {
        int scaledVelocityY = -1 * (int)(FLING_FACTOR * velocityY);  // Multiply by -1 to reverse natural scrolling
        scroller.fling(0, getCurrentIntValue(), 0, scaledVelocityY, 0, 0, minValue, maxValue);

        valueAnimator.setDuration(scroller.getDuration());
        valueAnimator.start();
    }

}