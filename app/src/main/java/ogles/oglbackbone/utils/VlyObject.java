package ogles.oglbackbone.utils;

import android.graphics.Color;
import android.graphics.ColorSpace;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

/**
 * This class represents a 3D object represented as a voxel set
 */
public class VlyObject {

    private int[] gridSize;
    private int voxelNum;

    // Represents each voxel pose as a 4d array (x, y, z)
    private float[][] voxel_poses;

    // Represent each voxel color as an index integer
    private int[] voxel_colors;

    // Associates a color_code to a Color
    private HashMap<Integer, Color> colorMap;

    // object inputStream, used for parsing
    private InputStream inputStream;

    // https://github.com/GeorgeAdamon/ways-to-render-1M-cubes/blob/master/README.md

    public VlyObject(InputStream inputStream){
        this.inputStream = inputStream;
        gridSize = new int[3];
        colorMap = new HashMap<>();
    }

    public void parse() throws IOException, NumberFormatException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        Iterator<String> it = reader.lines().iterator();
        String s;

        int voxelIndex = 0;

        while(it.hasNext()){
            s = it.next();

            if (s.contains("grid_size: ")) {
                // parse grid size
                int index = s.lastIndexOf("grid_size: ") + "grid_size: ".length();
                String[] sizes = s.substring(index).split(" ");
                for (int i = 0; i < sizes.length; i++) {
                    gridSize[i] = Integer.parseInt(sizes[i]);
                }
                Log.v("VLY_PARSER", "Grid size: " + gridSize[0] + " " + gridSize[1] + " " + gridSize[2] + " ");
            } else if (s.contains("voxel_num: ")) {
                // parse voxel count
                int index = s.lastIndexOf("voxel_num: ") + "voxel_num: ".length();
                voxelNum = Integer.parseInt(s.substring(index));
                // allocate buffers
                voxel_poses = new float[voxelNum][3];
                voxel_colors = new int[voxelNum];
                Log.v("VLY_PARSER", "Voxel count: " + voxelNum);
            } else {
                if (voxelIndex < voxelNum) {
                    // parse voxel
                    String[] v = s.split(" ");
                    for (int i = 0; i < 3; i++) {
                        voxel_poses[voxelIndex][i] = Float.parseFloat(v[i]);
                    }
                    voxel_colors[voxelIndex] = Integer.parseInt(v[3]);
                    Log.v("VLY_PARSER", "Parsed voxel #" + voxelIndex + " (" + Arrays.toString(voxel_poses[voxelIndex]) + ") - (color: " + voxel_colors[voxelIndex] + ")");
                } else {
                    // parse color row
                    String[] color = s.split(" ");
                    float[] floatColor = new float[4];

                    for (int i = 0; i < 3; i++) {
                        floatColor[i] = Float.parseFloat(color[i + 1]) / 255;
                    }
                    floatColor[3] = 1.0f; // alpha channel

                    Color c = Color.valueOf(floatColor, ColorSpace.get(ColorSpace.Named.SRGB));
                    colorMap.put(Integer.parseInt(color[0]), c);
                    Log.v("VLY_PARSER", "Parsed color #" + (voxelIndex - voxelNum) + " - " + c);
                }
                voxelIndex++;
            }
        }

        reader.close();
        inputStream.close();
//        buildMesh();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("Grid size: " + Arrays.toString(gridSize) + "\n");
        out.append("Voxel count: " + voxelNum + "\n");
        out.append("Voxels:\n");
        for (int j = 0; j < voxelNum; j++)
        {
            out.append("\t");
            for (int k = 0; k < 3; k++)
                out.append(voxel_poses[j][k] + " ");
            out.append(voxel_colors[j]);
            out.append("\n");
        }
        out.append("Colors:\n");
        for (Map.Entry<Integer, Color> e : colorMap.entrySet())
            out.append(e.getKey() + " " + e.getValue() + "\n");
        return out.toString();
    }

    public float[][] getVoxelPoses() {
        return voxel_poses;
    }

    public int[] getVoxelColors() {
        return voxel_colors;
    }

    public int getVoxelCount() {
        return this.voxelNum;
    }

//    /**
//     * Builds the mesh representation of the parsed object.
//     */
//    private void buildMesh() {
//        ArrayList<float[]> vertices = new ArrayList<>();
//        ArrayList<int[]> indices = new ArrayList<>();
//
//        // Create the faces
//        for (int v = 0; v < voxelNum; v++) {
//            int[] voxel = voxels[v];
//            Color voxelColor = colorMap.get(voxels[v][3]);
//            ArrayList<float[]> voxelVertices = new ArrayList<>();
//            ArrayList<int[]> voxelIndices = new ArrayList<>();
//
//
//            // add vertices
//            for (int x = voxel[0]; x < (voxel[0] + 1); x++)
//                for (int y = voxel[1]; y < (voxel[1] + 1); y++)
//                    for (int z = voxel[2]; z < (voxel[2] + 1); z++) {
//                        float[] vertex = new float[]{x, y, z};
//                        if (!vertices.contains(vertex)) {
//                            vertices.add(vertex);
//                            voxelVertices.add(vertex);
//                        }
//                    }
//
//            // add faces for the current voxel
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(1)), vertices.indexOf(voxelVertices.get(3)), vertices.indexOf(voxelVertices.get(7))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(1)), vertices.indexOf(voxelVertices.get(7)), vertices.indexOf(voxelVertices.get(5))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(2)), vertices.indexOf(voxelVertices.get(6))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(4)), vertices.indexOf(voxelVertices.get(6)), vertices.indexOf(voxelVertices.get(7))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(6)), vertices.indexOf(voxelVertices.get(4))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(4)), vertices.indexOf(voxelVertices.get(7)), vertices.indexOf(voxelVertices.get(5))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(2)), vertices.indexOf(voxelVertices.get(3))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(3)), vertices.indexOf(voxelVertices.get(1))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(1)), vertices.indexOf(voxelVertices.get(5))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(5)), vertices.indexOf(voxelVertices.get(4))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(4)), vertices.indexOf(voxelVertices.get(0)), vertices.indexOf(voxelVertices.get(1))});
//            voxelIndices.add(new int[]{vertices.indexOf(voxelVertices.get(4)), vertices.indexOf(voxelVertices.get(1)), vertices.indexOf(voxelVertices.get(5))});
//        }
//
//        Log.v("MESH_BUILDER", "Vertices: " + vertices);
//        Log.v("MESH_BUILDER", "Faces   : " + indices);
//
//        // TODO: cull triangles in the same position
//        // TODO: normalize vertex poses in the float [0-1] range?
//
//        // update vertices
//        this.vertices = new float[3 * vertices.size()];
//        for (int j = 0; j < vertices.size(); j++)
//            System.arraycopy(vertices.get(j), 0, this.vertices, j * 3, 3);
//
//        // update faces
//        this.faces = new int[3 * indices.size()];
//        for (int j = 0; j < indices.size(); j++)
//            System.arraycopy(indices.get(j), 0, this.faces, j * 3, 3);
//    }

}
