package com.nfaralli.particleflow;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.support.v8.renderscript.*;

/**
 * Renderer in charge of drawing the particles.
 * Computing the particles trajectory is quite expensive and slow in java, hence the use of
 * renderscript.
 * The loadShader and loadGlError methods are taken from a code sample of the Android tutorial:
 * http://developer.android.com/training/graphics/opengl/environment.html
 */
public class ParticlesRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "ParticlesRenderer";
    
    private FloatBuffer mPointVertices;
    private FloatBuffer mPointColors;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private SharedPreferences mPrefs;

    private int mProgram;
    private int maPositionHandle;
    private int maColorHandle;
    private int muMVPMatrixHandle;
    private int muPointSizeHandle;
    private int mWidth;
    private int mHeight;
    
    private RenderScript mRS;
    private ScriptC_particleflow mScript;
    private Boolean initialized = false;
    private Boolean posDirty = false;
    private Allocation indices;
    private Allocation touch;
    private Allocation position;
    private Allocation delta;
    private Allocation color;
    private int mNumTouch;
    private int mPartCount;
    private int mParticleSize;
    private float[] touchPos;
    private float[] pos;
    private float[] col;

    private final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform float uPointSize;" +
        "attribute vec4 aPosition;\n" +
        "attribute vec4 aColor;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  gl_PointSize = uPointSize;\n" +
        "  vColor = aColor;\n" +
        "}\n";	

    private final String mFragmentShader =
        "precision mediump float;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "  gl_FragColor = vColor;\n" +
        "}\n";
    
    public ParticlesRenderer(Context context) {
        mPrefs = context.getSharedPreferences(ParticlesSurfaceView.SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);
        mPartCount = mPrefs.getInt("NumParticles", ParticlesSurfaceView.DEFAULT_NUM_PARTICLES);
        mParticleSize = mPrefs.getInt("ParticleSize", ParticlesSurfaceView.DEFAULT_PARTICLE_SIZE);
        mNumTouch = mPrefs.getInt("NumAttPoints", ParticlesSurfaceView.DEFAULT_MAX_NUM_ATT_POINTS);
        touchPos = new float[2 * mNumTouch];
        pos = new float[2 * mPartCount];
        col = new float[4 * mPartCount];
        mPointVertices = ByteBuffer.allocateDirect(mPartCount * 2 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPointColors = ByteBuffer.allocateDirect(mPartCount * 4 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mRS = RenderScript.create(context);
        mScript = new ScriptC_particleflow(mRS);
    }

    public void onPrefsChanged() {
        mPartCount = mPrefs.getInt("NumParticles", ParticlesSurfaceView.DEFAULT_NUM_PARTICLES);
        mParticleSize = mPrefs.getInt("ParticleSize", ParticlesSurfaceView.DEFAULT_PARTICLE_SIZE);
        mNumTouch = mPrefs.getInt("NumAttPoints", ParticlesSurfaceView.DEFAULT_MAX_NUM_ATT_POINTS);
        touchPos = new float[2 * mNumTouch];
        pos = new float[2 * mPartCount];
        col = new float[4 * mPartCount];
        mPointVertices = ByteBuffer.allocateDirect(mPartCount * 2 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPointColors = ByteBuffer.allocateDirect(mPartCount * 4 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        int bgColor = mPrefs.getInt("BGColor", ParticlesSurfaceView.DEFAULT_BG_COLOR);
        float bgRed = Color.red(bgColor) / 255.f;
        float bgGreen = Color.green(bgColor) / 255.f;
        float bgBlue = Color.blue(bgColor) / 255.f;
        GLES20.glClearColor(bgRed, bgGreen, bgBlue, 1.0f);

        initialized = false;
        onSurfaceChanged(null, mWidth, mHeight);
    }
    
    // Set the position of the pointer 'index'.
    // This does NOT update Allocation touch (i.e. it does not update the script).
    // Use syncTouch() to update the Allocation touch with these new coordinates.
    public void setTouch(int index, float x, float y){
    	if(index >= mNumTouch) {
    		return;
    	}
    	index *=2;
        touchPos[index] = x;
        touchPos[index+1] = mHeight - y;
        posDirty = true;
    }
    
    // Sync the Allocation touch.
    public void syncTouch() {
    	if(!posDirty) {
    		return;
    	}
    	touch.copyFrom(touchPos);
    	posDirty = false;
    }

    /**
     * Creates the program based on the vertex and fragment shaders.
     */
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        int bgColor = mPrefs.getInt("BGColor", ParticlesSurfaceView.DEFAULT_BG_COLOR);
        float bgRed = Color.red(bgColor) / 255.f;
        float bgGreen = Color.green(bgColor) / 255.f;
        float bgBlue = Color.blue(bgColor) / 255.f;
        GLES20.glClearColor(bgRed, bgGreen, bgBlue, 1.0f);

        mProgram = createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        checkGlError("glGetAttribLocation aColor");
        if (maColorHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aColor");
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get uniform location for uMVPMatrix");
        }

        muPointSizeHandle = GLES20.glGetUniformLocation(mProgram, "uPointSize");
        if (muPointSizeHandle == -1) {
            throw new RuntimeException("Could not get uniform location for uPointSize");
        }
    }

    /**
     * Called when starting the app, after a pause/resume, or when the screen orientation changes.
     * Sets the initial attraction points and distributes all the particles uniformly over a disk
     * (by calling the initParticles function of the script).
     * It also allocates all the memory required for the script (Cf. Allocation.createSized()).
     */
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
    	mWidth = width;
    	mHeight = height;
        GLES20.glViewport(0, 0, width, height);

        Matrix.orthoM(mProjectionMatrix, 0, 0, -width, 0, height, 3, 7);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        
        if(mWidth == mScript.get_width() && mHeight == mScript.get_height() && initialized)
        	return; // onSurfaceChanged called after resuming the activity. check before reinitialize.
        mScript.set_width(mWidth);
        mScript.set_height(mHeight);
        float hsv[] = new float[3];
        Color.colorToHSV(mPrefs.getInt("SlowColor", ParticlesSurfaceView.DEFAULT_SLOW_COLOR), hsv);
        mScript.set_slowHue(hsv[0] / 360.f);
        mScript.set_slowSaturation(hsv[1]);
        mScript.set_slowValue(hsv[2]);
        Color.colorToHSV(mPrefs.getInt("FastColor", ParticlesSurfaceView.DEFAULT_FAST_COLOR), hsv);
        mScript.set_fastHue(hsv[0] / 360.f);
        mScript.set_fastSaturation(hsv[1]);
        mScript.set_fastValue(hsv[2]);
        mScript.set_hueDirection(mPrefs.getInt("HueDirection",
                ParticlesSurfaceView.DEFAULT_HUE_DIRECTION));
        mScript.set_f01AttractionCoef(mPrefs.getInt("F01Attraction",
                ParticlesSurfaceView.DEFAULT_F01_ATTRACTION_COEF));
        mScript.set_f01DragCoef(1 - mPrefs.getInt("F01Drag",
                ParticlesSurfaceView.DEFAULT_F01_DRAG_COEF)/100.f);
        if(!initialized) {
            int indices_[] = new int[mPartCount];
            indices = Allocation.createSized(mRS, Element.I32(mRS), mPartCount);
            for(int i=0; i<mPartCount; i++) {
            	indices_[i] = i;
            }
            indices.copyFrom(indices_);
            touch = Allocation.createSized(mRS, Element.F32_2(mRS), mNumTouch);
            position = Allocation.createSized(mRS, Element.F32_2(mRS), mPartCount);
            delta = Allocation.createSized(mRS, Element.F32_2(mRS), mPartCount);
            color = Allocation.createSized(mRS, Element.F32_4(mRS), mPartCount);
            mScript.bind_gTouch(touch);
            mScript.bind_position(position);
            mScript.bind_delta(delta);
            mScript.bind_color(color);
            initialized = true;
        }
        float l = (mWidth < mHeight ? mWidth : mHeight) / 3;
        setTouch(0, mWidth / 2, mHeight / 2 + (mNumTouch == 1 ? 0 : l));
        for(int i=1; i<mNumTouch; i++){
        	setTouch(i,
        			(float)(mWidth/2 + l*Math.sin(i*2*Math.PI/mNumTouch)),
        			(float)(mHeight/2 + l*Math.cos(i*2*Math.PI/mNumTouch)));
        }
        syncTouch();

        mScript.invoke_initParticles();
    }

    /**
     * Update and draw the particles.
     */
    @Override
    public void onDrawFrame(GL10 unused) {
        // Draw background color.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
 
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1f(muPointSizeHandle, mParticleSize);

        mScript.forEach_updateParticles(indices);
        // There might be a better way to copy an Allocation to a FloatBuffer...
        position.copyTo(pos);
        mPointVertices.position(0);
        mPointVertices.put(pos);
        mPointVertices.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 8, mPointVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        color.copyTo(col);
        mPointColors.position(0);
        mPointColors.put(col);
        mPointColors.position(0);
        GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false, 16, mPointColors);
        checkGlError("glVertexAttribPointer maColor");
        GLES20.glEnableVertexAttribArray(maColorHandle);
        
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mPartCount);
        checkGlError("glDrawArrays");
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
    * Utility method for debugging OpenGL calls. Provide the name of the call
    * just after making it:
    *
    * <pre>
    * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
    * ParticlesRenderer.checkGlError("glGetUniformLocation");</pre>
    *
    * If the operation is not successful, the check throws an error.
    *
    * @param glOperation - Name of the OpenGL call to check.
    */
    public void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}