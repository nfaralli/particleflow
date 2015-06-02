package com.nfaralli.particleflow;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
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

    private static final int PART_COUNT = 50000; // Count of particles
    public static final int NUM_TOUCH = 5; // Number of screen touches taken into account
    
    private FloatBuffer mPointVertices;
    private FloatBuffer mPointColors;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private int mProgram;
    private int maPositionHandle;
    private int maColorHandle;
    private int muMVPMatrixHandle;
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
    private float[] touchPos = new float[2*NUM_TOUCH];
    private float[] pos = new float[2*PART_COUNT];
    private float[] col = new float[4*PART_COUNT];

    private final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec4 aColor;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vColor = aColor;\n" +
        "}\n";	

    private final String mFragmentShader =
        "precision mediump float;\n" +
        "varying vec4 vColor;\n" +
        "void main() {\n" +
        "  gl_FragColor = vColor;\n" +
        "}\n";
    
    public ParticlesRenderer(Context context) {
        mPointVertices = ByteBuffer.allocateDirect(PART_COUNT * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPointColors = ByteBuffer.allocateDirect(PART_COUNT * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mRS = RenderScript.create(context);
        mScript = new ScriptC_particleflow(mRS);
    }
    
    // Set the position of the pointer 'index'.
    // This does NOT update Allocation touch (i.e. it does not update the script).
    // Use syncTouch() to update the Allocation touch with these new coordinates.
    public void setTouch(int index, float x, float y){
    	if(index >= NUM_TOUCH) {
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
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
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
        if(!initialized) {
            int indices_[] = new int[PART_COUNT];
            indices = Allocation.createSized(mRS, Element.I32(mRS), PART_COUNT);
            for(int i=0; i<PART_COUNT; i++) {
            	indices_[i] = i;
            }
            indices.copyFrom(indices_);
            touch = Allocation.createSized(mRS, Element.F32_2(mRS), NUM_TOUCH);
            position = Allocation.createSized(mRS, Element.F32_2(mRS), PART_COUNT);
            delta = Allocation.createSized(mRS, Element.F32_2(mRS), PART_COUNT);
            color = Allocation.createSized(mRS, Element.F32_4(mRS), PART_COUNT);
            mScript.bind_gTouch(touch);
            mScript.bind_position(position);
            mScript.bind_delta(delta);
            mScript.bind_color(color);
            initialized = true;
        }
        float l = (mWidth < mHeight ? mWidth : mHeight) / 3;
        setTouch(0, mWidth/2, mHeight/2 + (NUM_TOUCH==1?0:l));
        for(int i=1; i<NUM_TOUCH; i++){
        	setTouch(i,
        			(float)(mWidth/2 + l*Math.sin(i*2*Math.PI/NUM_TOUCH)),
        			(float)(mHeight/2 + l*Math.cos(i*2*Math.PI/NUM_TOUCH)));
        }
        syncTouch();
        
        mScript.invoke_initParticles();
    }

    /**
     * Update and draw the particles.
     */
    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
 
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

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
        
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, PART_COUNT);
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