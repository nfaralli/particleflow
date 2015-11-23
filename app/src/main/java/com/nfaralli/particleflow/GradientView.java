package com.nfaralli.particleflow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GradientView extends View {

    private int mLeftColor;
    private int mRightColor;
    private boolean mHueClockwise;
    private Bitmap mBitmap;
    private Matrix mIdentityMatrix;


    public GradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLeftColor = 0;
        mRightColor = 0;
        mHueClockwise = true;
        mIdentityMatrix = new Matrix();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        createBitmap(w, h);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, mIdentityMatrix, null);
    }

    public void setLeftColor(int color) {
        mLeftColor = color;
        createBitmap(getWidth(), getHeight());
    }

    public void setRightColor(int color) {
        mRightColor = color;
        createBitmap(getWidth(), getHeight());
    }

    public void setHueDirection(boolean clockwise) {
        mHueClockwise = clockwise;
        createBitmap(getWidth(), getHeight());
    }

    private void createBitmap(int w, int h) {
        if (w<=0 || h<=0) {
            return;
        }
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);
        Paint paint = new Paint();
        float leftHSV[] = new float[3];
        float rightHSV[] = new float[3];
        float hsv[] = new float[3];
        float alpha;

        Color.colorToHSV(mLeftColor, leftHSV);
        Color.colorToHSV(mRightColor, rightHSV);
        for(int x=0; x<w; x++) {
            alpha = ((float)x)/(w-1);
            hsv[0] = getHue(alpha, leftHSV[0], rightHSV[0], mHueClockwise);
            hsv[1] = (1 - alpha) * leftHSV[1] + alpha * rightHSV[1];
            hsv[2] = (1 - alpha) * leftHSV[2] + alpha * rightHSV[2];
            paint.setColor(Color.HSVToColor(hsv));
            canvas.drawLine(x, 0, x, h, paint);
        }
    }

    private float getHue(float alpha, float left, float right, boolean clockwise) {
        float hue;
        if (left < right && clockwise) {
            left += 360;
        } else if (left > right && !clockwise) {
            right += 360;
        }
        hue = (1 - alpha) * left + alpha * right;
        if (hue >= 360) {
            hue -= 360;
        }
        return hue;
    }
}
