package com.benoithiller.textwave;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final String SEEN_HELP = "com.benoithiller.textwave.SEEN_HELP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);
    }

    public void showMessage(View view) {
        EditText scrollText = (EditText) findViewById(R.id.scroll_text);
        String text = scrollText.getText().toString();
        if (text.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid message")
                    .setMessage("You didn't type a message to display. That won't work at all.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }).show();
        } else {

            ToggleButton capsToggle = (ToggleButton) findViewById(R.id.all_caps_toggle);
            if (capsToggle.isChecked()) {
                text = text.toUpperCase();
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean seenHelp = preferences.getBoolean(SEEN_HELP, false);
            Intent intent;
            if (seenHelp) {
                intent = new Intent(this, TextScrollerActivity.class);

            } else {
                intent = new Intent(this, HelpActivity.class);
            }

            intent.putExtra(TextScrollerActivity.SCROLL_STRING, text);

            ToggleButton darkModeToggle = (ToggleButton) findViewById(R.id.dark_mode_toggle);
            intent.putExtra(TextScrollerActivity.DARK_MODE, darkModeToggle.isChecked());

            int armLength = preferences.getInt("arm_length_preference", R.integer.default_arm_length);
            ToggleButton longRangeToggle = (ToggleButton) findViewById(R.id.long_range_toggle);
            if (!longRangeToggle.isChecked()) {
                armLength = armLength / 2;
            }

            intent.putExtra(TextScrollerActivity.ARM_LENGTH, armLength);

            intent.putExtra(TextScrollerActivity.VIBRATE, preferences.getBoolean("vibrate_preference", true));

            if (seenHelp) {
                startActivity(intent);
            } else {
                startActivityForResult(intent, 1);
            }
        }
    }

    public void showHelp(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(SEEN_HELP, true);
            editor.apply();

            Intent intent = new Intent(data);
            intent.setClass(this, TextScrollerActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
