package com.nfaralli.particleflow;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ColorView extends FrameLayout {

    public interface OnValueChangedListener {
        void onValueChanged(int color);
    }

    private ValidatedEditText mRedText;
    private ValidatedEditText mGreenText;
    private ValidatedEditText mBlueText;
    private OnValueChangedListener mOnValueChangedListener;

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addView(inflate(getContext(), R.layout.color, null));
        mRedText = (ValidatedEditText)findViewById(R.id.bgColorR);
        mGreenText = (ValidatedEditText)findViewById(R.id.bgColorG);
        mBlueText = (ValidatedEditText)findViewById(R.id.bgColorB);

        ValidatedEditText.OnTextChangedListener onTextChangedListener =
                new ValidatedEditText.OnTextChangedListener() {
                    @Override
                    public void onTextChanged(String str) {
                        onValueChanged();
                    }
                };
        mRedText.setOnTextChangedListener(onTextChangedListener);
        mRedText.setMinValue(0);
        mRedText.setMaxValue(255);
        mGreenText.setOnTextChangedListener(onTextChangedListener);
        mGreenText.setMinValue(0);
        mGreenText.setMaxValue(255);
        mBlueText.setOnTextChangedListener(onTextChangedListener);
        mBlueText.setMinValue(0);
        mBlueText.setMaxValue(255);
        setColor(0xFF000000);
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mOnValueChangedListener = listener;
    }

    public void onValueChanged() {
        if (mOnValueChangedListener != null) {
            mOnValueChangedListener.onValueChanged(getColor());
        }
    }

    public int getColor() {
        int r = Integer.parseInt(mRedText.getText().toString());
        int g = Integer.parseInt(mGreenText.getText().toString());
        int b = Integer.parseInt(mBlueText.getText().toString());
        return Color.rgb(r, g, b);
    }

    public void setColor(int color) {
        mRedText.setText(String.valueOf(Color.red(color)));
        mGreenText.setText(String.valueOf(Color.green(color)));
        mBlueText.setText(String.valueOf(Color.blue(color)));
        onValueChanged();
    }
}
