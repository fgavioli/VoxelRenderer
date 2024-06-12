package ogles.oglbackbone.utils;

public class Mesh {

    protected float vertices[] = null;
    protected int faces[] = null;

    /**
     * Returns the mesh vertices of the object
     * @return the vertices
     */
    public float[] getVertices() {
        return vertices;
    }

    /**
     * Returns the mesh faces of the object
     * @return the faces
     */
    public int[] getIndices() {
        return faces;
    }


}
