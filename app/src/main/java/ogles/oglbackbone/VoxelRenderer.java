package ogles.oglbackbone;

import static android.opengl.GLES20.GL_FRONT_AND_BACK;
import static android.opengl.GLES20.GL_GEQUAL;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glUseProgram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
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
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES30.glVertexAttribDivisor;
import static java.lang.Float.max;


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
            4, 5, 6,
            6, 7, 4,
            4, 5, 1,
            1, 0, 4,
            6, 7, 3,
            3, 2, 6,
            4, 0, 3,
            3, 7, 4,
            1, 5, 6,
            6, 2, 1
    };

    private int shaderHandle;

    private int colorTableloc;

    private int VAO[];
    private int VBO[];

    private float viewM[];
    private float modelM[][];
    private float projM[];
    private float MVP[][];

    private VlyObject obj;
    private float[] modelCenter;
    private int modelRotDeg;

    public VoxelRenderer(VlyObject obj) {
        super(0.25f, 0.25f, 0.25f, 1);
        this.obj = obj;

        viewM = new float[16];
        modelM = new float[obj.getVoxelCount()][16];
        projM = new float[16];
        MVP = new float[obj.getVoxelCount()][16];
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

        for (int i = 0; i < obj.getVoxelCount(); i++) {
            Matrix.setIdentityM(modelM[i], 0);
            Matrix.translateM(modelM[i], 0,
                    voxelPoses[i][0] - modelCenter[0],
                    voxelPoses[i][1] - modelCenter[1],
                    voxelPoses[i][2] - modelCenter[2]);
            Matrix.rotateM(//TODO: ROT);
        }

        generateMVPMatrices();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        // TODO: add here the movement listener to change perspective
        this.surface.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float xPerc = event.getX() / v.getWidth();
                    if (xPerc > 0.8)
                        modelRotDeg += 15; // rotate right
                    else if (xPerc < 0.2)
                        modelRotDeg -= 15; // rotate left
                    // regen MVP matrices for the voxels
                    generateMVPMatrices();
                }
                return true;
            }
        });
    }

    private void generateMVPMatrices() {
        float[][] voxelPoses = obj.getVoxelPoses();
        for (int i = 0; i < voxelPoses.length; i++) {
            float[] tempM = new float[16];
            Matrix.multiplyMM(tempM, 0, viewM, 0, modelM[i], 0);
            Matrix.multiplyMM(MVP[i], 0, projM, 0, tempM, 0);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));
        float fovVertical = 45f;
        float fovHorizontal = (float) (2.0 * (1 / Math.tan(Math.tan(fovVertical / 2) * aspect)));

        float maxHorizontalSize = Float.max(modelCenter[0], modelCenter[2]);
        float cameraDistance = (maxHorizontalSize) /
                (2 * (float)(Math.tan((fovHorizontal * Math.PI / 180) / 2)));
        float zNear = cameraDistance - maxHorizontalSize;
        float zFar = cameraDistance + maxHorizontalSize;

        Matrix.perspectiveM(projM, 0, fovVertical, aspect, zNear, zFar);
        Matrix.setLookAtM(viewM, 0, 0f, 0f, cameraDistance,
                0f, 0f, 0f,
                0, 1, 0);
        generateMVPMatrices();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        String vertexSrc = "#version 300 es\n" +
                "\n" +
                "layout (location = 1) in vec3 vPos;\n" +
                "layout (location = 2) in int vColor;\n" +
                "layout (location = 3) in mat4 iMVP;\n" +
                "flat out int voxelColor;\n" +
                "\n" +
                "void main() {\n" +
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

        shaderHandle = ShaderCompiler.createProgram(vertexSrc, fragmentSrc);

        VAO = new int[1]; // one VAO for vPos
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
        GLES30.glBindVertexArray(0);

        colorTableloc = glGetUniformLocation(shaderHandle, "colorTable");

        glDepthFunc(GL_LEQUAL);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GLES20.GL_DEPTH_TEST);

        glUseProgram(shaderHandle);

        GLES30.glBindVertexArray(VAO[0]);

        // generate MVP buffer
        FloatBuffer MVPBuffer =
                ByteBuffer.allocateDirect(obj.getVoxelCount() * 16 * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        for (int i = 0; i < obj.getVoxelCount(); i++) {
            MVPBuffer.put(MVP[i]);
        }
        MVPBuffer.position(0);

        // bind MVP buffer
        glBindBuffer(GL_ARRAY_BUFFER, VBO[3]);
        glBufferData(GL_ARRAY_BUFFER, MVPBuffer.capacity() * 4, MVPBuffer, GL_STATIC_DRAW);
        // pass 4x4 mat to attrib.
        // Src: https://stackoverflow.com/questions/28595598/glm-mat4x4-to-layout-qualifier
        for (int i = 0; i < 4; i++) {
            glEnableVertexAttribArray(3 + i);
            glVertexAttribPointer(3 + i, 4, GL_FLOAT, false, 16 * 4, i * 16);
            glVertexAttribDivisor(3 + i, 1);
        }

        GLES30.glUniform3fv(colorTableloc, obj.getColorCount(), obj.getColorTable(), 0);

        // Draw!
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, voxelIndices.length, GLES30.GL_UNSIGNED_INT, 0, obj.getVoxelCount());

        GLES30.glBindVertexArray(0);
        glUseProgram(0);
    }
}
