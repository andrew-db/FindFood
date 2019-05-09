package com.krakenjaws.findfood.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.krakenjaws.findfood.R;

public class MainActivity extends AppCompatActivity {
    // This will help us when debugging
    private static final String TAG = "MainActivity";

    // Here we place our widgets


    // Here we place our variables


    /**
     * Our first view, try to keep this clean
     *
     * @param savedInstanceState What is this
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}
