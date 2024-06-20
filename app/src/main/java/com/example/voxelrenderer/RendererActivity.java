package com.example.voxelrenderer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;

import ogles.oglbackbone.VoxelRenderer;
import ogles.oglbackbone.utils.VlyObject;

public class RendererActivity extends Activity {

    private String modelName;
    private GLSurfaceView surface;
    private boolean isSurfaceCreated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();

        int supported = 1;

        if(configurationInfo.reqGlEsVersion>=0x30000)
            supported = 3;
        else if(configurationInfo.reqGlEsVersion>=0x20000)
            supported = 2;

        Log.v("TAG","Opengl ES supported >= " +
                supported + " (" + Integer.toHexString(configurationInfo.reqGlEsVersion) + " " +
                configurationInfo.getGlEsVersion() + ")");

        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(supported);
        surface.setPreserveEGLContextOnPause(true);

        Bundle extras = getIntent().getExtras();
        if (extras == null)
            finish(); // exit
        else
            modelName = extras.getString("modelName");

        // load model
        VlyObject model = null;
        try {
            model = new VlyObject(this.getAssets().open(modelName + ".vly"));
            model.parse();
        } catch (IOException e) {
            Log.v("MAIN","Unable to parse asset " + modelName);
        }

        if (model == null) {
            finish();
            return;
        }

        VoxelRenderer renderer = new VoxelRenderer(model);
        renderer.setContextAndSurface(this,surface);
        surface.setRenderer(renderer);
        setContentView(surface);
        isSurfaceCreated = true;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(isSurfaceCreated)
            surface.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(isSurfaceCreated)
            surface.onPause();
    }
}