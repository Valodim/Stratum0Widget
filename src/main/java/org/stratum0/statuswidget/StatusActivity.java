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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.stratum0.statuswidget.GlobalVars.TAG;

/**
 * Created by tsuro on 9/1/13.
 */
public class StatusActivity extends Activity implements Button.OnClickListener {

    private SharedPreferences prefs;
    SpaceStatus status;
    EditText nameBox;
    ToggleButton toggleButton;
    Button inheritButton;
    TextView currentUser;

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
                    Log.d(TAG, "Could not parse status response.");
                } catch (InterruptedException e) {
                }
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            toggleButton.setEnabled(false);
            inheritButton.setEnabled(false);
            StatusActivity.this.setProgressBarIndeterminate(true);
            StatusActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            updateActivityInfo();
            if(!success) {
                Log.e(TAG, "Status update failed.");
                Toast.makeText(StatusActivity.this, getText(R.string.updateFailed), Toast.LENGTH_LONG).show();
            }
            toggleButton.setEnabled(true);
            if(!nameBox.getText().toString().equals(status.getOpenedBy())) {
                inheritButton.setEnabled(status.isOpen());
            }
            StatusActivity.this.setProgressBarIndeterminate(false);
            StatusActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.status_layout);

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        inheritButton = (Button) findViewById(R.id.inheritButton);
        nameBox = (EditText) findViewById(R.id.editText);
        currentUser = (TextView) findViewById(R.id.textView);
        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        status = new SpaceStatus();

        toggleButton.setOnClickListener(this);
        inheritButton.setOnClickListener(this);
        nameBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if(status.isOpen()) {
                    inheritButton.setEnabled(!nameBox.getText().toString().equals(status.getOpenedBy()));
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", editable.toString());
                editor.commit();
            }
        });

        String username = prefs.getString("username", "DooRMasteR");
        nameBox.setText(username);
    }

    private void updateActivityInfo() {
        try {
            status.update();
        } catch (ParseException e) {
            Log.e(TAG, "Could not parse space status.", e);
            return;
        }

        Log.d(TAG, "Open?  " + status.isOpen());
        Log.d(TAG, "Opened by: " + status.getOpenedBy());
        Log.d(TAG, "Open since: " + status.getSince());

        toggleButton.setChecked(status.isOpen());
        if(!nameBox.getText().toString().equals(status.getOpenedBy())) {
            inheritButton.setEnabled(status.isOpen());
        }
        if(status.isOpen()) {
            currentUser.setText(status.getSince() + " (" + status.getOpenedBy() + ")");
        } else {
            currentUser.setText(status.getSince().toString());
        }
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
            Log.e(TAG, "Update request: malformed URL.", e);
            return;
        } catch (IOException e) {
            Log.e(TAG, "Update request: could not connect to server.", e);
            return;
        }

        new SuccessCheckTask().execute(b);
    }

}
