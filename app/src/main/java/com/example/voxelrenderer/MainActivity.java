package com.example.voxelrenderer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button simple = findViewById(R.id.buttonSimple);
        Button chrk = findViewById(R.id.buttonChrk);
        Button dragon = findViewById(R.id.buttonDragon);
        Button monu2 = findViewById(R.id.buttonMonu2);
        Button monu16 = findViewById(R.id.buttonMonu16);
        Button christmas = findViewById(R.id.buttonChristmas);

        View.OnClickListener startRenderer = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button source = (Button) v;
                String modelName = (String) source.getText();
                Intent i = new Intent(MainActivity.this, RendererActivity.class);
                i.putExtra("modelName", modelName);
                startActivity(i);
            }
        };
        simple.setOnClickListener(startRenderer);
        chrk.setOnClickListener(startRenderer);
        dragon.setOnClickListener(startRenderer);
        monu2.setOnClickListener(startRenderer);
        monu16.setOnClickListener(startRenderer);
        christmas.setOnClickListener(startRenderer);
    }
}