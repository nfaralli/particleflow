package com.nfaralli.particleflow;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GearView extends ImageView {

    // Array of resource IDs for the gear animation. A negative index is used to indicate that the
    // gear is hidden.
    private int mGearIndex;
    private final int mGearIds[] = {
            R.drawable.gear_icon_00,
            R.drawable.gear_icon_10,
            R.drawable.gear_icon_20,
            R.drawable.gear_icon_30,
            R.drawable.gear_icon_40,
            R.drawable.gear_icon_50};

    // Timer used to hide the gear after 4 seconds.
    private final CountDownTimer mCountDownTimer = new CountDownTimer(4000, 4000) {
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            hideGear();
        }
    };

    // Handler used to animate the gear.
    private final Handler mHandler = new Handler();
    private final Runnable mDrawGear = new Runnable() {
        @Override
        public void run() {
            updateGear();
        }
    };

    public GearView(Context context, AttributeSet attrs) {
        super(context, attrs);
        hideGear();
    }

    public boolean isGearVisible() {
        return mGearIndex >= 0;  // Negative index <=> gear is hidden.
    }

    public void showGear() {
        if (mGearIndex < 0) {
            mGearIndex = 0;
        }
        mCountDownTimer.start();
        updateGear();
    }

    public void hideGear() {
        mCountDownTimer.cancel();
        mHandler.removeCallbacks(mDrawGear);
        mGearIndex = -1;
        setImageResource(R.drawable.gear_icon_empty);
    }

    private void updateGear() {
        setImageResource(mGearIds[mGearIndex]);
        mGearIndex = (mGearIndex + 1) % mGearIds.length;
        mHandler.removeCallbacks(mDrawGear);
        mHandler.postDelayed(mDrawGear, 1000 / 30);  // 30 updates per second.
    }
}
