package com.nfaralli.particleflow;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * View container used to draw OpenGL points (particles).
 * This view creates and set its renderer (in charge of drawing the particles)
 * and is in charge of capturing the touch events (to move the attraction points).
 */
public class ParticlesSurfaceView extends GLSurfaceView
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SHARED_PREFS_NAME="particleFlowPrefs";
    public static final int DEFAULT_NUM_PARTICLES = 50000;
    public static final int MAX_NUM_PARTICLES = 1000000;
    public static final int DEFAULT_PARTICLE_SIZE = 1;
    public static final int DEFAULT_MAX_NUM_ATT_POINTS = 5;
    public static final int MAX_MAX_NUM_ATT_POINTS = 16;
    public static final int DEFAULT_BG_COLOR = 0xFF000000;
    public static final int DEFAULT_SLOW_COLOR = 0xFF4C4CFF;
    public static final int DEFAULT_FAST_COLOR = 0xFFFF4C4C;
    public static final int DEFAULT_HUE_DIRECTION = 0;
    public static final int DEFAULT_F01_ATTRACTION_COEF = 100;
    public static final int DEFAULT_F01_DRAG_COEF = 4;

    private final ParticlesRenderer mRenderer;
    // The count array is a hack to activate or deactivate an attraction point.
    // Each touch has its own counter in this array, which is incremented at each touch event.
    // If the touch is present during one touch event, then a new attraction point is created.
    // If the touch is not present for at least 3 consecutive touch event, then the corresponding
    // attraction point is deactivated.
    // This is necessary when moving several attraction points simultaneously and lifting all the
    // fingers at once, which usually results in several touch events, not just one.
    private int mCount[];
    private final SharedPreferences mPrefs;

    public ParticlesSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create an OpenGL ES 2.0 context.
        // Don't forget to set the following line in the manifest:
        // <uses-feature android:glEsVersion="0x00020000" android:required="true" />
        // Also, don't use setPreserveEGLContextOnPause, or double check that a change to the
        // background color via the settings menu still works.
        setEGLContextClientVersion(2);

        // Create and set the Renderer for drawing on the GLSurfaceView
        mRenderer = new ParticlesRenderer(context);
        setRenderer(mRenderer);

        // Get the shared preferences and create the counter array.
        mPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mCount = new int[mPrefs.getInt("NumAttPoints", DEFAULT_MAX_NUM_ATT_POINTS)];
    }

    @Override
    public void onResume() {
        super.onResume();
        // use sticky immersive mode (available only for API 19 and above).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
    	int numPointers;
        int index, id, ids;

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            	ids = 0;
            	numPointers = e.getPointerCount();
                // Get the list of touch IDs and check for current or new attraction points.
            	for(index = 0; index < numPointers; index++) {
            		id = e.getPointerId(index);
            		ids |= 1 << id;
            		if(id < mCount.length) {
            		    mCount[id] = 0;
      	        	    mRenderer.setTouch(id, e.getX(index), e.getY(index));
            		}
            	}
                // Check which attraction points should be deactivated.
            	for(id = 0; id < mCount.length; id++, ids >>= 1){
                	if ((ids & 1) == 0) {
                    	if(mCount[id]++ >= 3){
                            // Negative coordinates are used to deactivate an attraction point.
              	        	mRenderer.setTouch(id, -1.0f, -1.0f);
                    	}
                	}
            	}
                // syncTouch() must be used to update the script.
            	mRenderer.syncTouch();
                requestRender();
                break;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == "ShowSettingsHint") {
            return;
        }
        mCount = new int[mPrefs.getInt("NumAttPoints", DEFAULT_MAX_NUM_ATT_POINTS)];
        mRenderer.onPrefsChanged();
    }

    public void resetAttractionPoints(){
        mRenderer.resetAttractionPoints();
    }
}
