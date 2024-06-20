package ogles.oglbackbone.utils;

import static java.lang.Math.max;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class represents a 3D object in occupancy grid form.
 */
public class VlyObject {

    private int[] gridSize;
    private int voxelNum;
    private int colorNum;

    // Represents each voxel pose as a 3d array (x, y, z)
    private float[][] voxel_poses;

    // Associates at each index the corresponding RGB color
    private float[] colorTable;

    // Represent each voxel color as an integer index of the color table
    private int[] voxel_colors;

    // object inputStream, used for parsing
    private InputStream inputStream;

    public VlyObject(InputStream inputStream){
        this.inputStream = inputStream;
        gridSize = new int[3];
        colorTable = null;
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
            } else if (s.contains("voxel_num: ")) {
                // parse voxel count
                int index = s.lastIndexOf("voxel_num: ") + "voxel_num: ".length();
                voxelNum = Integer.parseInt(s.substring(index));
                // allocate buffers
                voxel_poses = new float[voxelNum][3];
                voxel_colors = new int[voxelNum];
            } else {
                if (voxelIndex < voxelNum) {
                    // parse voxel
                    String[] v = s.split(" ");
                    voxel_poses[voxelIndex][0] = Float.parseFloat(v[0]);
                    voxel_poses[voxelIndex][1] = Float.parseFloat(v[2]);
                    voxel_poses[voxelIndex][2] = Float.parseFloat(v[1]);
                    voxel_colors[voxelIndex] = Integer.parseInt(v[3]);
                } else {
                    if (colorTable == null)
                    {
                        int highest_idx = 0;
                        for (int i = 0; i < voxelNum; i++)
                            highest_idx = max(voxel_colors[i], highest_idx);
                        colorTable = new float[(highest_idx + 1) * 3];
                        colorNum = 0;
                    }
                    // parse color row
                    String[] color = s.split(" ");

                    for (int i = 0; i < 3; i++) {
                        colorTable[colorNum * 3 + i] = Float.parseFloat(color[i + 1]) / 255;
                    }
                    colorNum++;
                }
                voxelIndex++;
            }
        }

        reader.close();
        inputStream.close();
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
        for (int i = 0; i < colorNum; i++)
            out.append(i + " (" + colorTable[i * 3] + ", "
                    + colorTable[i * 3 + 1] + ", "
                    + colorTable[i * 3 + 2] + ", "
                    + ")\n");
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

    public float[] getColorTable() {
        return colorTable;
    }

    public int getColorCount() {
        return colorNum;
    }
}
