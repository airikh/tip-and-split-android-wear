package com.abraxascreative.tipandsplit.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.abraxascreative.tipandsplit.R;

public class RemotelyScrollableInteger extends TextView {
    private int initialValue;
    private int minValue;
    private int maxValue;
    private boolean doesRoundRobin;
    private String formatString;
    private float scrollFactor;

    private OnIntValueChangeListener onIntValueChangeListener;

    private float currentFloatValue;  // Keeps track of scroll position, which maps to integer via Math.round

    public RemotelyScrollableInteger(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RemotelyScrollableInteger, 0, 0);

        try {
            initialValue = a.getInt(R.styleable.RemotelyScrollableInteger_initialValue, 0);
            minValue = a.getInt(R.styleable.RemotelyScrollableInteger_minValue, 0);
            maxValue = a.getInt(R.styleable.RemotelyScrollableInteger_maxValue, 0);
            doesRoundRobin = a.getBoolean(R.styleable.RemotelyScrollableInteger_doesRoundRobin, false);
            scrollFactor = a.getFloat(R.styleable.RemotelyScrollableInteger_scrollFactor, 0.05f);

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
        setCurrentFloatValue((float) initialValue, true); // Force update/redraw, in case initialValue == 0
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
            updateText();
            // Also call the listener, if one was set
            if (onIntValueChangeListener != null) {
                onIntValueChangeListener.onIntValueChange(getCurrentIntValue());
            }
        }
    }

    public void scroll(float distanceY) {
        float newValue = currentFloatValue + distanceY * scrollFactor;
        setCurrentFloatValue(newValue);
    }

}