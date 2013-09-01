package org.stratum0.statuswidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by tsuro on 9/1/13.
 */
public class StatusActivity extends Activity implements Button.OnClickListener, TextWatcher {

    private SharedPreferences prefs;
    SpaceStatus status;
    EditText nameBox;
    ToggleButton toggleButton;

    private class SuccessCheckTask extends AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Boolean... expected) {
            for(int i = 0; i < 10; ++i) {
                try {
                    status.update();
                    if(status.isOpen() == expected[0])
                        return true;
                    Thread.sleep(500);
                } catch (ParseException e) {
                    Log.d(StratumsphereStatusProvider.TAG, "Could not parse status response.");
                } catch (InterruptedException e) {
                }
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            toggleButton.setEnabled(false);
            StatusActivity.this.setProgressBarIndeterminate(true);
            StatusActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            updateActivityInfo();
            if(!success) {
                Log.e(StratumsphereStatusProvider.TAG, "Status update failed.");
                Toast.makeText(StatusActivity.this, "Status update failed.", Toast.LENGTH_LONG).show();
            }
            toggleButton.setEnabled(true);
            StatusActivity.this.setProgressBarIndeterminate(false);
            StatusActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.status_layout);

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        nameBox = (EditText) findViewById(R.id.editText);
        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        toggleButton.setOnClickListener(this);
        nameBox.addTextChangedListener(this);

        String username = prefs.getString("username", "DooRMasteR");
        nameBox.setText(username);

        status = new SpaceStatus();
    }

    private void updateActivityInfo() {
        try {
            status.update();
        } catch (ParseException e) {
            Log.e(StratumsphereStatusProvider.TAG, "Could not parse space status.", e);
            return;
        }

        Log.d(StratumsphereStatusProvider.TAG, "Open?  " + status.isOpen());
        Log.d(StratumsphereStatusProvider.TAG, "Opened by: " + status.getOpenedBy());
        Log.d(StratumsphereStatusProvider.TAG, "Open since: " + status.getSince());

        toggleButton.setChecked(status.isOpen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateActivityInfo();
    }

    @Override
    public void onClick(View view) {
        String new_status;
        URL u;
        boolean b = toggleButton.isChecked();
        if(b) {
            new_status = "open%20"+nameBox.getText();
        } else {
            new_status = "close";
        }

        try {
            u = new URL("http://auge-faust.de/trixie/say/sudo%20" + new_status);
            URLConnection c = u.openConnection();
            c.connect();
            c.getContent();
        } catch (MalformedURLException e) {
            Log.e(StratumsphereStatusProvider.TAG, "Update request: malformed URL.", e);
            return;
        } catch (IOException e) {
            Log.e(StratumsphereStatusProvider.TAG, "Update request: could not connect to server.", e);
            return;
        }

        new SuccessCheckTask().execute(b);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", editable.toString());
        editor.commit();
    }
}
