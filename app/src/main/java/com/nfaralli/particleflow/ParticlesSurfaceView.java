package com.nfaralli.particleflow;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

/**
 * View container used to draw OpenGL points (particles).
 * This view creates and set its renderer (in charge of drawing the particles)
 * and is in charge of capturing the touch events (to move the attraction points).
 */
public class ParticlesSurfaceView extends GLSurfaceView {

    private final ParticlesRenderer mRenderer;
    // The count array is a hack to activate or deactivate an attraction point.
    // Each touch has its own counter in this array, which is incremented at each touch event.
    // If the touch is present during one touch event, then a new attraction point is created.
    // If the touch is not present for at least 3 consecutive touch event, then the corresponding
    // attraction point is deactivated.
    // This is necessary when moving several attraction points simultaneously and lifting all the
    // fingers at once, which usually results in several touch events, not just one.
    private int count[] = new int[ParticlesRenderer.NUM_TOUCH];

    public ParticlesSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        // Don't forget to set the following line in the manifest:
        // <uses-feature android:glEsVersion="0x00020000" android:required="true" />
        setEGLContextClientVersion(2);

        // Create and set the Renderer for drawing on the GLSurfaceView
        mRenderer = new ParticlesRenderer(context);
        setRenderer(mRenderer);

        // Set the initial counters to 0.
        for(int i=0; i < count.length; i++) {
        	count[i] = 0;
        }

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
            		if(id < ParticlesRenderer.NUM_TOUCH) {
            		    count[id] = 0;
      	        	    mRenderer.setTouch(id, e.getX(index), e.getY(index));
            		}
            	}
                // Check which attraction points should be deactivated.
            	for(id = 0; id < ParticlesRenderer.NUM_TOUCH; id++, ids >>= 1){
                	if ((ids & 1) == 0) {
                    	if(count[id]++ >= 3){
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
}
