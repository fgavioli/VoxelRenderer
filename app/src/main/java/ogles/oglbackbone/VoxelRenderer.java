package ogles.oglbackbone;

import static android.opengl.GLES20.GL_GEQUAL;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glUseProgram;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

    private int MVPloc;
    private int colorLoc;

    private int VAO[];

    private float viewM[];
    private float modelM[][];
    private float projM[];
    private float MVP[][];

    private VlyObject obj;

    public VoxelRenderer(VlyObject obj) {
        super(0.25f, 0.25f, 0.25f, 1);
        this.obj = obj;

        viewM = new float[16];
        modelM = new float[obj.getVoxelCount()][16];
        projM = new float[16];
        MVP = new float[obj.getVoxelCount()][16];
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);
        for (int i = 0; i < obj.getVoxelCount(); i++)
            Matrix.setIdentityM(modelM[i], 0);
        generateMVPMatrices();
    }

    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        // TODO: add here the movement listener to change perspective
    }

    private void generateMVPMatrices() {
        float[][] voxelPoses = obj.getVoxelPoses();
        for (int i = 0; i < voxelPoses.length; i++) {
            float[] model = new float[16];
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, voxelPoses[i][0], voxelPoses[i][1], voxelPoses[i][2]);

            float[] tempM = new float[16];
            Matrix.multiplyMM(tempM, 0, viewM, 0, model, 0);
            Matrix.multiplyMM(MVP[i], 0, projM, 0, tempM, 0);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewM, 0, -50f, -50f, 0f,
                0, 0, 0,
                0, 1, 0);
        generateMVPMatrices();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);

        String vertexSrc = "#version 300 es\n" +
                "\n" +
                "layout(location = 1) in vec3 vPos;\n" +
                "uniform mat4 mvpMatrices[1000];\n" +
                "\n" +
                "void main() {\n" +
                "    gl_Position = mvpMatrices[gl_InstanceID] * vec4(vPos, 1.0);\n" +
                "}";
        String fragmentSrc = "#version 300 es\n" +
                "\n" +
                "precision mediump float;\n" +
                "\n" +
                "uniform vec3 colorUni;\n" +
                "out vec4 fragColor;\n" +
                "\n" +
                "void main() {\n" +
                "fragColor = vec4(colorUni,1);\n" +
                "}\n";

        shaderHandle = ShaderCompiler.createProgram(vertexSrc, fragmentSrc);

        VAO = new int[1]; // one VAO for vPos

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

        int VBO[] = new int[2]; //0: vPos, 1: faces

        GLES30.glGenVertexArrays(1, VAO, 0);
        glGenBuffers(2, VBO, 0);

        // bind vertices pose buffer
        GLES30.glBindVertexArray(VAO[0]);
        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(),
                vertexData, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        // bind indices buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData,
                GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);

        MVPloc = glGetUniformLocation(shaderHandle, "mvpMatrices");
        colorLoc = glGetUniformLocation(shaderHandle, "colorUni");

        glDepthFunc(GL_LEQUAL);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GLES20.GL_DEPTH_TEST);

        glUseProgram(shaderHandle);

        // Set uniform values
        float[] mvpMatrixArray = new float[obj.getVoxelCount() * 16];
        for (int i = 0; i < obj.getVoxelCount(); i++) {
            System.arraycopy(MVP[i], 0, mvpMatrixArray, i * 16, 16);
        }
        GLES30.glUniformMatrix4fv(MVPloc, obj.getVoxelCount(), false, mvpMatrixArray, 0);

        // Bind VAO and draw instances
        GLES30.glBindVertexArray(VAO[0]);
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, voxelIndices.length, GLES30.GL_UNSIGNED_INT, 0, obj.getVoxelCount());

        // color
        glUniform3f(colorLoc, 1, 0, 0);

        GLES30.glBindVertexArray(0);
        glUseProgram(0);
    }
}
