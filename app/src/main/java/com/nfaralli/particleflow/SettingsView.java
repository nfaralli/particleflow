package com.nfaralli.particleflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;

public class SettingsView extends FrameLayout {

    private ValidatedEditText mNumParticles;
    private ValidatedEditText mParticleSize;
    private ValidatedEditText mNumAttPoints;
    private ValidatedEditText mF01Attraction;
    private ValidatedEditText mF01Drag;
    private ColorView mBGColor;
    private ColorView mSlowPColor;
    private ColorView mFastPColor;
    private GradientView mBGGradientView;
    private GradientView mPartGradientView;
    private Spinner mHueDirection;
    private SharedPreferences mPrefs;

    public SettingsView(Context context) {
        super(context);
        addView(inflate(getContext(), R.layout.settings, null));
        mNumParticles = (ValidatedEditText)findViewById(R.id.numParticles);
        mNumParticles.setMinValue(1);
        mNumParticles.setMaxValue(ParticlesSurfaceView.MAX_NUM_PARTICLES);
        mParticleSize = (ValidatedEditText)findViewById(R.id.particleSize);
        mParticleSize.setMinValue(1);
        mParticleSize.setMaxValue(50);
        mNumAttPoints = (ValidatedEditText)findViewById(R.id.numAPoints);
        mNumAttPoints.setMinValue(1);
        mNumAttPoints.setMaxValue(ParticlesSurfaceView.MAX_MAX_NUM_ATT_POINTS);
        mBGColor = (ColorView)findViewById(R.id.bgColor);
        mSlowPColor = (ColorView)findViewById(R.id.slowColor);
        mFastPColor = (ColorView)findViewById(R.id.fastColor);
        mBGGradientView = (GradientView)findViewById(R.id.bgGradientView);
        mPartGradientView = (GradientView)findViewById(R.id.gradientView);
        mHueDirection = (Spinner)findViewById(R.id.hueDirection);
        mF01Attraction = (ValidatedEditText)findViewById(R.id.f01_attraction);
        mF01Attraction.setMinValue(0);
        mF01Attraction.setMaxValue(1000);
        mF01Drag = (ValidatedEditText)findViewById(R.id.f01_drag);
        mF01Drag.setMinValue(0);
        mF01Drag.setMaxValue(100);
        mPrefs = context.getSharedPreferences(ParticlesSurfaceView.SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);

        mBGColor.setOnValueChangedListener(new ColorView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int color) {
                mBGGradientView.setLeftColor(color);
                mBGGradientView.setRightColor(color);
                mBGGradientView.invalidate();
            }
        });
        mSlowPColor.setOnValueChangedListener(new ColorView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int color) {
                mPartGradientView.setLeftColor(color);
                mPartGradientView.invalidate();
            }
        });
        mFastPColor.setOnValueChangedListener(new ColorView.OnValueChangedListener() {
            @Override
            public void onValueChanged(int color) {
                mPartGradientView.setRightColor(color);
                mPartGradientView.invalidate();
            }
        });
        mHueDirection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPartGradientView.setHueDirection(position == 0);
                mPartGradientView.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDefaultValues();
            }
        });

        loadValues();
    }

    public void loadValues() {
        mNumParticles.setText(String.valueOf(mPrefs.getInt("NumParticles",
                ParticlesSurfaceView.DEFAULT_NUM_PARTICLES)));
        mParticleSize.setText(String.valueOf(mPrefs.getInt("ParticleSize",
                ParticlesSurfaceView.DEFAULT_PARTICLE_SIZE)));
        mNumAttPoints.setText(String.valueOf(mPrefs.getInt("NumAttPoints",
                ParticlesSurfaceView.DEFAULT_MAX_NUM_ATT_POINTS)));
        mBGColor.setColor(mPrefs.getInt("BGColor", ParticlesSurfaceView.DEFAULT_BG_COLOR));
        mSlowPColor.setColor(mPrefs.getInt("SlowColor", ParticlesSurfaceView.DEFAULT_SLOW_COLOR));
        mFastPColor.setColor(mPrefs.getInt("FastColor", ParticlesSurfaceView.DEFAULT_FAST_COLOR));
        mHueDirection.setSelection(mPrefs.getInt("HueDirection",
                ParticlesSurfaceView.DEFAULT_HUE_DIRECTION));
        mF01Attraction.setText(String.valueOf(mPrefs.getInt("F01Attraction",
                ParticlesSurfaceView.DEFAULT_F01_ATTRACTION_COEF)));
        mF01Drag.setText(String.valueOf(mPrefs.getInt("F01Drag",
                ParticlesSurfaceView.DEFAULT_F01_DRAG_COEF)));
    }

    public void loadDefaultValues() {
        mNumParticles.setText(String.valueOf(ParticlesSurfaceView.DEFAULT_NUM_PARTICLES));
        mParticleSize.setText(String.valueOf(ParticlesSurfaceView.DEFAULT_PARTICLE_SIZE));
        mNumAttPoints.setText(String.valueOf(ParticlesSurfaceView.DEFAULT_MAX_NUM_ATT_POINTS));
        mBGColor.setColor(ParticlesSurfaceView.DEFAULT_BG_COLOR);
        mSlowPColor.setColor(ParticlesSurfaceView.DEFAULT_SLOW_COLOR);
        mFastPColor.setColor(ParticlesSurfaceView.DEFAULT_FAST_COLOR);
        mHueDirection.setSelection(ParticlesSurfaceView.DEFAULT_HUE_DIRECTION);
        mF01Attraction.setText(String.valueOf(ParticlesSurfaceView.DEFAULT_F01_ATTRACTION_COEF));
        mF01Drag.setText(String.valueOf(ParticlesSurfaceView.DEFAULT_F01_DRAG_COEF));
    }

    public void saveValues() {
        // By clearing the focus first, we force the EditText which has the focus to lose it and
        // therefore to validate its text before saving its value.
        View focusedChild = getFocusedChild();
        if (focusedChild!= null) {
            focusedChild.clearFocus();
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("NumParticles", Integer.parseInt(mNumParticles.getText().toString()));
        editor.putInt("ParticleSize", Integer.parseInt(mParticleSize.getText().toString()));
        editor.putInt("NumAttPoints", Integer.parseInt(mNumAttPoints.getText().toString()));
        editor.putInt("BGColor", mBGColor.getColor());
        editor.putInt("SlowColor", mSlowPColor.getColor());
        editor.putInt("FastColor", mFastPColor.getColor());
        editor.putInt("HueDirection", mHueDirection.getSelectedItemPosition());
        editor.putInt("F01Attraction", Integer.parseInt(mF01Attraction.getText().toString()));
        editor.putInt("F01Drag", Integer.parseInt(mF01Drag.getText().toString()));
        editor.commit();
    }
}
