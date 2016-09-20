package com.benoithiller.textwave;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * Activity for the help screen
 */
public class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);
    }

    public void close(View view) {
        setResult(RESULT_OK, getIntent());
        finish();
    }
}
