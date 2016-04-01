package com.abraxascreative.tipandsplit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.abraxascreative.tipandsplit.views.RemotelyScrollableInteger;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class MainActivity extends Activity {
    private RemotelyScrollableInteger billDollarsView, billCentsView, tipPercentView, numSplittingView;  // (aka RSI's)
    private TextView tipAmountView, totalPerPersonView;
    private GestureOverlayView gestureOverlayView;

    private SharedPreferences sharedPreferences;

    private GestureDetector gestureDetector;

    private DinnerCheck dinnerCheck;

    private SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {
        private RemotelyScrollableInteger focusedRSI;

        @Override
        public boolean onDown(MotionEvent e) {
            // Continue listening to gesture if currently focused view is one of the RemotelyScrollableInteger's
            View focusedView = MainActivity.this.getCurrentFocus();
            if (focusedView instanceof RemotelyScrollableInteger) {
                focusedRSI = (RemotelyScrollableInteger) focusedView;  // Set for use in onScroll and onFling callbacks
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            focusedRSI.scroll(distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            focusedRSI.fling(velocityY);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                billDollarsView = (RemotelyScrollableInteger) stub.findViewById(R.id.bill_dollars);
                billCentsView = (RemotelyScrollableInteger) stub.findViewById(R.id.bill_cents);
                tipPercentView = (RemotelyScrollableInteger) stub.findViewById(R.id.tip_percent);
                numSplittingView = (RemotelyScrollableInteger) stub.findViewById(R.id.num_splitting);
                tipAmountView = (TextView) stub.findViewById(R.id.tip_amount);
                totalPerPersonView = (TextView) stub.findViewById(R.id.total_per_person);
                gestureOverlayView = (GestureOverlayView) stub.findViewById(R.id.scroll_area);

                // Initialize with stored values; use current value (as set by RSI initialValue attribute) as default
                billDollarsView.setIntValue(sharedPreferences.getInt(getString(R.string.saved_bill_dollars),
                                                                     billDollarsView.getCurrentIntValue()));
                billCentsView.setIntValue(sharedPreferences.getInt(getString(R.string.saved_bill_cents),
                                                                   billCentsView.getCurrentIntValue()));
                tipPercentView.setIntValue(sharedPreferences.getInt(getString(R.string.saved_tip_percent),
                                                                    tipPercentView.getCurrentIntValue()));

                // Initialize dinnerCheck, now that the view references are set and can provide their int values
                dinnerCheck = new DinnerCheck();

                setUpListeners();
            }
        });
    }

    private void setUpListeners(){
        gestureDetector = new GestureDetector(MainActivity.this, simpleOnGestureListener);
        gestureOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = gestureDetector.onTouchEvent(event);
                if (!result) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        View focusedView = MainActivity.this.getCurrentFocus();
                        if (focusedView instanceof RemotelyScrollableInteger) {
                            ((RemotelyScrollableInteger) focusedView).snapToInt();  // Ensures consistent scroll dist.
                            result = true;
                        }
                    }
                }
                return result;
            }
        });

        billDollarsView.setOnIntValueChangeListener(new RemotelyScrollableInteger.OnIntValueChangeListener() {
            @Override
            public void onIntValueChange(int newValue) {
                dinnerCheck.setBillDollars(newValue);
            }
        });
        billCentsView.setOnIntValueChangeListener(new RemotelyScrollableInteger.OnIntValueChangeListener() {
            @Override
            public void onIntValueChange(int newValue) {
                dinnerCheck.setBillCents(newValue);
            }
        });
        tipPercentView.setOnIntValueChangeListener(new RemotelyScrollableInteger.OnIntValueChangeListener() {
            @Override
            public void onIntValueChange(int newValue) {
                dinnerCheck.setTipPercent(newValue);
            }
        });
        numSplittingView.setOnIntValueChangeListener(new RemotelyScrollableInteger.OnIntValueChangeListener() {
            @Override
            public void onIntValueChange(int newValue) {
                dinnerCheck.setNumSplitting(newValue);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Store user's current values (to restore on relaunch)
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.saved_bill_dollars), billDollarsView.getCurrentIntValue());
        editor.putInt(getString(R.string.saved_bill_cents), billCentsView.getCurrentIntValue());
        editor.putInt(getString(R.string.saved_tip_percent), tipPercentView.getCurrentIntValue());
        editor.commit();
    }


    private class DinnerCheck {
        private int billDollars, billCents, tipPercent, numSplitting;
        private double billAmount, tipAmount, totalPerPerson;
        private NumberFormat formatter;

        public DinnerCheck() {
            formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumIntegerDigits(1);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            formatter.setRoundingMode(RoundingMode.HALF_UP);

            billDollars = billDollarsView.getCurrentIntValue();
            billCents = billCentsView.getCurrentIntValue();
            tipPercent = tipPercentView.getCurrentIntValue();
            numSplitting = numSplittingView.getCurrentIntValue();

            updateBillAmount();
        }

        public void setBillCents(int billCents) {
            this.billCents = billCents;
            updateBillAmount();
        }

        public void setBillDollars(int billDollars) {
            this.billDollars = billDollars;
            updateBillAmount();
        }

        public void setTipPercent(int tipPercent) {
            this.tipPercent = tipPercent;
            updateTipAmount();
        }

        public void setNumSplitting(int numSplitting) {
            this.numSplitting = numSplitting;
            updateTotalPerPerson();
        }

        private void updateBillAmount() {
            billAmount = (double)billDollars + (double)billCents * 0.01d;
            updateTipAmount();
            updateTotalPerPerson();
        }

        private void updateTipAmount() {
            tipAmount = billAmount * (double)tipPercent * 0.01d;
            updateTotalPerPerson();
            tipAmountView.setText(formatter.format(tipAmount));
        }

        private void updateTotalPerPerson() {
            totalPerPerson = (billAmount + tipAmount) / (double)numSplitting;
            totalPerPersonView.setText(formatter.format(totalPerPerson));
        }
    }

}
