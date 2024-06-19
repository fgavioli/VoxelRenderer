package ogles.oglbackbone;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glUseProgram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ogles.oglbackbone.utils.ShaderCompiler;
import ogles.oglbackbone.utils.VlyObject;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.glVertexAttribDivisor;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;

public class VoxelRenderer extends BasicRenderer {

    // vertices for the generic cubical voxel, centered at (0, 0, 0)
    private static final float[] voxelVertices = {
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            0.5f, -0.5f,  0.5f,
            0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f
    };

    // triangular faces for the generic voxel instance
    private static final int[] voxelIndices = {
            0, 1, 2,
            2, 3, 0,
            4, 6, 5,
            6, 4, 7,
            4, 5, 1,
            1, 0, 4,
            6, 7, 3,
            3, 2, 6,
            4, 0, 3,
            3, 7, 4,
            1, 5, 6,
            6, 2, 1
    };

    private static final float fingerToCameraRatio = 0.15f;

    private int shaderHandle;

    private int colorTableloc;
    private int VPMatrixLoc;

    private int VAO[];
    private int VBO[];

    private float viewM[];
    private float modelM[][];
    private float projM[];
    private float VP[];

    private VlyObject obj;
    private float[] modelCenter;
    private int viewRotDeg;
    private boolean VPRegen;
    private float cameraDistance;

    private float currentFingerDistance;
    private float startingCameraDistance;
    private final float fovVerticalDeg = 45f;
    private float fovHorizontalRad;
    private float maxHorizontalSize;
    private float aspectRatio;
    private FloatBuffer modelMatBuffer;

    public VoxelRenderer(VlyObject obj) {
        super(0.25f, 0.25f, 0.25f, 1);
        this.obj = obj;

        viewM = new float[16];
        modelM = new float[obj.getVoxelCount()][16];
        projM = new float[16];
        VP = new float[16];
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);

        float[][] voxelPoses = obj.getVoxelPoses();

        float[] min = Arrays.copyOf(voxelPoses[0], voxelPoses[0].length);
        float[] max = Arrays.copyOf(voxelPoses[0], voxelPoses[0].length);
        for (int i = 1; i < obj.getVoxelCount(); i++) {
            for (int j = 0; j < 3; j++) {
                min[j] = Math.min(voxelPoses[i][j], min[j]);
                max[j] = Math.max(voxelPoses[i][j], max[j]);
            }
        }

        // calculate model center
        modelCenter = new float[3];
        for (int j = 0; j < 3; j++) {
            modelCenter[j] = (min[j] + max[j]) / 2.0f;
        }

        // precompute model matrices
        for (int i = 0; i < voxelPoses.length; i++) {
            Matrix.setIdentityM(modelM[i], 0);
            Matrix.translateM(modelM[i], 0,
                    voxelPoses[i][0] - modelCenter[0],
                    voxelPoses[i][1] - modelCenter[1],
                    voxelPoses[i][2] - modelCenter[2]);
        }

        viewRotDeg = 0;
        VPRegen = true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        this.surface.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getPointerCount() == 2) {
                    float dx = event.getX(0) - event.getX(1);
                    float dy = event.getY(0) - event.getY(1);
                    float fingerDistance = (float) sqrt(dx * dx + dy * dy);
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            // save starting distance
                            startingCameraDistance = cameraDistance;
                            currentFingerDistance = fingerDistance;
                            break;

                        case MotionEvent.ACTION_MOVE:
                            // update distance based on movement
                            float deltaFingerDistance = (currentFingerDistance - fingerDistance);
                            float newCameraDistance = startingCameraDistance + (deltaFingerDistance * fingerToCameraRatio);
                            updateCameraDistance(newCameraDistance);
                            VPRegen = true;
                            break;

                        default:
                            break;
                    }
                    Log.v("touchListener", "cameraDistance: " + cameraDistance);
                } else if (event.getPointerCount() == 1) {
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        float xPerc = event.getX() / v.getWidth();
                        if (xPerc > 0.90) {
                            viewRotDeg = (viewRotDeg + 15) % 360;
                            VPRegen = true;
                        } else if (xPerc < 0.10) {
                            viewRotDeg = (viewRotDeg - 15) % 360;
                            VPRegen = true;
                        }
                    }
                }
                return true;
            }
        });
    }

    private void generateVPMatrix() {
        float eyeX = (float) (cameraDistance * sin(toRadians(viewRotDeg)));
        float eyeZ = (float) (cameraDistance * cos(toRadians(viewRotDeg)));

        Matrix.setLookAtM(viewM, 0, eyeX, 0f, eyeZ,
                0f, 0f, 0f,
                0, 1, 0);

        Matrix.multiplyMM(VP, 0, projM, 0, viewM, 0);
    }

    private void updateCameraDistance(float newCameraDistance) {
        float zNear = newCameraDistance - maxHorizontalSize/2;
        float zFar = newCameraDistance + maxHorizontalSize/2;
        float nearFrustumWidth = (float) (2.0f * zNear * tan(fovHorizontalRad)) * aspectRatio;
        if (zNear < 0 || nearFrustumWidth > maxHorizontalSize * 2)
            return;
        cameraDistance = newCameraDistance;
        Matrix.perspectiveM(projM, 0, fovVerticalDeg, aspectRatio, zNear, zFar);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        aspectRatio = ((float) w) / ((float) (h == 0 ? 1 : h));
        float fovVerticalRad = (float) (fovVerticalDeg * (Math.PI / 180));
        // hFov = 2 * atan(tan(vFov/2) * AR)
        fovHorizontalRad = (float) (2.0 * (Math.atan(tan(fovVerticalRad / 2) * aspectRatio)));

        float modelWidthX = modelCenter[0] * 2;
        float modelWidthZ = modelCenter[2] * 2;
        maxHorizontalSize = (float) sqrt((modelWidthX * modelWidthX) + (modelWidthZ * modelWidthZ));

        // camera distance is calculated as the distance at which the frustum plane has a width
        // equal to the maximum horizontal size of the model.
        // d = (modelMaxHSize / (2tan(fovHorizontal))) + (maxHorizontalSize / 2)
        float newCameraDistance =  (float) ((maxHorizontalSize) /
                (2 * (tan(fovHorizontalRad / 2)))) + maxHorizontalSize/2;
        updateCameraDistance(newCameraDistance);
        VPRegen = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        glEnable(GLES20.GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        String vertexSrc = "#version 300 es\n" +
                "\n" +
                "layout (location = 1) in vec3 vPos;\n" +
                "layout (location = 2) in int vColor;\n" +
                "layout (location = 3) in mat4 iMMatrix;\n" +
                "uniform mat4 VPMatrix;\n" +
                "flat out int voxelColor;\n" +
                "\n" +
                "void main() {\n" +
                "    mat4 iMVP = VPMatrix * iMMatrix;\n" +
                "    gl_Position = iMVP * vec4(vPos, 1.0);\n" +
                "    voxelColor = vColor;\n" +
                "}";

        String fragmentSrc = "#version 300 es\n" +
                "\n" +
                "precision mediump float;\n" +
                "\n" +
                "flat in int voxelColor;\n" +
                "uniform vec3 colorTable[" + obj.getColorCount() + "];\n" +
                "out vec4 colorOut;\n" +
                "\n" +
                "void main() {\n" +
                "    colorOut = vec4(colorTable[voxelColor], 1);\n" +
                "}\n";

        //TODO: implement lighting

        shaderHandle = ShaderCompiler.createProgram(vertexSrc, fragmentSrc);

        VAO = new int[1]; // one VAO for voxel vPos
        GLES30.glGenVertexArrays(1, VAO, 0);

        // generate vertex buffer
        FloatBuffer vertexData =
                ByteBuffer.allocateDirect(voxelVertices.length * Float.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        vertexData.put(voxelVertices);
        vertexData.position(0);

        // generate indices buffer
        IntBuffer indexData =
                ByteBuffer.allocateDirect(voxelIndices.length * Integer.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();
        indexData.put(voxelIndices);
        indexData.position(0);

        // generate color buffer
        IntBuffer colorBuffer = ByteBuffer.allocateDirect(obj.getVoxelColors().length * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        colorBuffer.put(obj.getVoxelColors());
        colorBuffer.position(0);

        // generate MVP buffer
        modelMatBuffer =
                ByteBuffer.allocateDirect(obj.getVoxelCount() * 16 * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();

        VBO = new int[4]; //0: vPos, 1: vIndices, 2: vColor, 3: iMVP
        glGenBuffers(4, VBO, 0);

        GLES30.glBindVertexArray(VAO[0]);

        // bind vertices pose buffer
        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(),
                vertexData, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        // bind indices buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData,
                GL_STATIC_DRAW);

        // bind colors buffer (per instance)
        glBindBuffer(GL_ARRAY_BUFFER, VBO[2]);
        glBufferData(GL_ARRAY_BUFFER, colorBuffer.capacity() * 4, colorBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);
        // Set the attribute divisor to change color every instance
        glVertexAttribDivisor(2, 1);

        // bind model buffer
        glBindBuffer(GL_ARRAY_BUFFER, VBO[3]);
        for (int i = 0; i < obj.getVoxelCount(); i++) {
            modelMatBuffer.put(modelM[i]);
        }
        modelMatBuffer.position(0);
        glBufferData(GL_ARRAY_BUFFER, modelMatBuffer.capacity() * 4, modelMatBuffer, GL_DYNAMIC_DRAW);
        // declare 4x4 mat as vertex attribute
        // Src: https://stackoverflow.com/questions/28595598/glm-mat4x4-to-layout-qualifier
        for (int i = 0; i < 4; i++) {
            glEnableVertexAttribArray(3 + i);
            glVertexAttribPointer(3 + i, 4, GL_FLOAT, false, 16 * 4, i * 16);
            glVertexAttribDivisor(3 + i, 1);
        }

        colorTableloc = glGetUniformLocation(shaderHandle, "colorTable");
        VPMatrixLoc   = glGetUniformLocation(shaderHandle, "VPMatrix");
        GLES30.glBindVertexArray(0);

        glDepthFunc(GL_LEQUAL);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        long start = SystemClock.elapsedRealtime();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(shaderHandle);
        GLES30.glBindVertexArray(VAO[0]);

        // update VP uniform buffer if needed
        if (VPRegen) {
            Log.v("VP", "Updating VP buffer...");
            generateVPMatrix();
            VPRegen = false;
        }

        GLES30.glUniformMatrix4fv(VPMatrixLoc, 1, false, VP, 0);
        GLES30.glUniform3fv(colorTableloc, obj.getColorCount(), obj.getColorTable(), 0);

        // Draw instances
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, voxelIndices.length, GLES30.GL_UNSIGNED_INT, 0, obj.getVoxelCount());
        long end = SystemClock.elapsedRealtime();
        Log.v("TIMING", "Drawcall: " + (end-start));

        GLES30.glBindVertexArray(0);
        glUseProgram(0);
    }
}
