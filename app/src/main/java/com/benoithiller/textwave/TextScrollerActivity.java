package com.benoithiller.textwave;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

/**
 * Activity that shows the text scroller
 */
public class TextScrollerActivity extends Activity implements SensorEventListener {
    public static final String SCROLL_STRING = "com.benoithiller.textwave.SCROLL_STRING";
    public static final String DARK_MODE = "com.benoithiller.textwave.DARK_MODE";
    public static final String ARM_LENGTH = "com.benoithiller.textwave.ARM_LENGTH";
    public static final String VIBRATE = "com.benoithiller.textwave.VIBRATE";

    private SensorManager sensorManager;
    private Sensor gravity;
    private float[] gravityValues;
    private TextScroller scroller;

    private Vector2 up;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        Intent intent = getIntent();
        String scrollText = intent.getStringExtra(SCROLL_STRING);

        boolean darkMode = intent.getBooleanExtra(DARK_MODE, false);

        boolean vibrate = intent.getBooleanExtra(VIBRATE, true);

        int armLength = intent.getIntExtra(ARM_LENGTH, R.integer.default_arm_length);

        scroller = new TextScroller(this);
        scroller.setText(scrollText);
        if (darkMode) {
            scroller.setForegroundColor(Color.WHITE);
            scroller.setBackgroundColor(Color.BLACK);
        }
        scroller.setPadding(0, 30, 0, 30);
        scroller.setArmLength(armLength);
        scroller.setVibrate(vibrate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scroller.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        setContentView(scroller);
    }

    private void moveMessage(double position) {
        scroller.move(position);
    }

    private Vector2 getUp(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return Vector2.j.mult(Math.signum(gravityValues[1]));
        } else {
            return Vector2.i.mult(Math.signum(gravityValues[0]));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        gravityValues = lowPass(event.values.clone(), gravityValues);
        if (up == null) {
            up = getUp(getResources().getConfiguration().orientation);
        }
        Vector3 vector = new Vector3(gravityValues[0], gravityValues[1], gravityValues[2]);
        Vector2 vectorXY = vector.flatten();
        double angle = vectorXY.angle(up);
        moveMessage(angle);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static float[] lowPass(float[] input, float[] output) {
        float alpha = 0.7f;
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

}
