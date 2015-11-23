package com.nfaralli.particleflow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * EditText class with min and max values.
 * The text is validated when the user presses the Done button, or when the focus is lost.
 */
public class ValidatedEditText extends EditText implements TextView.OnEditorActionListener,
        View.OnFocusChangeListener{

    public interface OnTextChangedListener {
        void onTextChanged(String str);
    }

    private int mMinValue;
    private int mMaxValue;
    private int mInitialValue;  // Initial value of the EditText when it gets the focus.
    private OnTextChangedListener mOnTextChangedListener;

    public ValidatedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMinValue = 0;
        mMaxValue = 0;
        mInitialValue = 0;

        // Both listener are needed:
        // In case of an ACTION_DONE action the focus doesn't change but the value still needs
        // to be validated, hence the OnEditorActionListener.
        // Having just this listener handling also ACTION_NEXT wouldn't be enough as the text
        // wouldn't be validated if the user switched to another View without pressing the Done or
        // Next button. Therefore we still need the OnFocusChangeListener.
        setOnEditorActionListener(this);
        setOnFocusChangeListener(this);
    }

    public void setMinValue(int minValue) {
        mMinValue = minValue;
    }

    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
    }

    public void setOnTextChangedListener(OnTextChangedListener listener) {
        mOnTextChangedListener = listener;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            validateText((EditText) v);
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            mInitialValue = Integer.parseInt(((EditText) v).getText().toString());
        } else {
            validateText((EditText) v);
        }
    }

    // Make sure the value contained in the EditText is valid, change it otherwise.
    private void validateText(EditText v) {
        Integer value;
        String initialValueStr = v.getText().toString();
        if (initialValueStr.equals("")) {
            value = mInitialValue;
        } else {
            try {
                value = Integer.valueOf(initialValueStr);
            } catch (NumberFormatException e) {
                value = mMinValue;
            }
        }
        if (value < mMinValue) {
            value = mMinValue;
        } else if (value > mMaxValue) {
            value = mMaxValue;
        }
        // Change only if the text is different.
        if (!initialValueStr.equals(value.toString())) {
            v.setText(value.toString());
        }
        if (mOnTextChangedListener != null) {
            mOnTextChangedListener.onTextChanged(value.toString());
        }
    }
}
