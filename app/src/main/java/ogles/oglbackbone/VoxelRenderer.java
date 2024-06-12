package ogles.oglbackbone;

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

    // vertices for the generic voxel instance
    private static final float[] voxel_vertices = {
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 1.0f
    };

    // triangular faces for the generic voxel instance
    private static final int[] voxel_indices = {
            0, 6, 4,
            0, 2, 6,
            0, 3, 2,
            0, 1, 3,
            2, 7, 6,
            2, 3, 7,
            4, 6, 7,
            4, 7, 5,
            0, 4, 5,
            0, 5, 1,
            1, 5, 7,
            1, 7, 3
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
        super(0, 0, 0, 1);
        this.obj = obj;

        viewM = new float[16];
        modelM = new float[obj.getVoxelCount()][16];
        projM = new float[16];
        MVP = new float[obj.getVoxelCount()][16];
        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);
        for (int i = 0; i < obj.getVoxelCount(); i++)
            Matrix.setIdentityM(modelM[i], 0);
    }

    @Override
    public void setContextAndSurface(Context context, GLSurfaceView surface) {
        super.setContextAndSurface(context, surface);

        // TODO: add here the movement listener to change perspective
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        super.onSurfaceChanged(gl10, w, h);
        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewM, 0, 0, 0f, 14f,
                0, 0, 0,
                0, 1, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        super.onSurfaceCreated(gl10, eglConfig);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        String vertexSrc = "#version 300 es\n" +
                "\n" +
                "layout(location = 1) in vec3 vPos;\n" +
                "uniform mat4 MVP;\n" +
                "\n" +
                "void main(){\n" +
                "gl_Position = MVP * vec4(vPos,1);\n" +
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

        VAO = new int[1]; //one VAO to bind  vpos

        // copy vertices
        FloatBuffer vertexData =
                ByteBuffer.allocateDirect(voxel_vertices.length * Float.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        vertexData.put(voxel_vertices);
        vertexData.position(0);

        // copy indices
        IntBuffer indexData =
                ByteBuffer.allocateDirect(voxel_indices.length * Integer.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer();
        indexData.put(voxel_indices);
        indexData.position(0);

        int VBO[] = new int[2]; //0: instance_vertices, 1: instance_indices

        glGenBuffers(2, VBO, 0);

        GLES30.glGenVertexArrays(1, VAO, 0);

        // bind vertices pose buffer
        GLES30.glBindVertexArray(VAO[0]);
        glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
        glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexData.capacity(),
                vertexData, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0); //vpos tightly packed
        glEnableVertexAttribArray(1);

        // bind indices buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexData.capacity(), indexData,
                GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);

        MVPloc = glGetUniformLocation(shaderHandle, "MVP");
        colorLoc = glGetUniformLocation(shaderHandle, "colorUni");

        glDepthFunc(GL_LEQUAL);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GLES20.GL_DEPTH_TEST);

        glUseProgram(shaderHandle);

    }
}
