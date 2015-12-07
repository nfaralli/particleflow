package com.nfaralli.particleflow;

import android.content.Context;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Service used by the live wallpaper.
 * The engine uses a modified ParticleSurfaceView to draw on the wallpaper.
 */
public class ParticleFlowWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    class WallpaperEngine extends Engine {
        private WPSurfaceView mGLView;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mGLView = new WPSurfaceView(ParticleFlowWallpaperService.this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mGLView.resetAttractionPoints();
                mGLView.onResume();
            } else {
                mGLView.onPause();
            }
        }

        @Override
        public void onDestroy() {
            mGLView.onPause();
            super.onDestroy();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            mGLView.onTouchEvent(event);
        }

        // Create a simple subclass of ParticlesSurfaceView and override getHolder in order to
        // draw on the correct surface.
        class WPSurfaceView extends ParticlesSurfaceView {
            public WPSurfaceView(Context context) {
                super(context, null);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }
        }
    }
}
