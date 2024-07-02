# VoxelRenderer

This project implements an OpenGLES renderer for a preloaded set of vly-formatted 3D objects.

The project is based on instanced rendering, with a single cube instance instanced in a different position for each voxel in the object file.

## Classes
- `VlyObject.java` - Parser class for the `.vly` files, represents the 3D object as a set of cubes
- `VoxelRenderer.java` - Voxel Renderer class that implements the rendering.
- `MainActivity.java` - Launcher activity showing the main menu with the list of renderable objects
- `RendererActivity.java` - Secondary activity which renders the chosen model, passed via Android bundles

## App Usage
Click the button with the name of the model you want to render and wait for it to render on the display.

After that, you can touch the sides of the screen to move the camera around the object or pinch to zoom closer or farther from the object.

------------

Author: Federico Gavioli
