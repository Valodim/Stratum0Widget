package org.stratum0.statuswidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;

import static org.stratum0.statuswidget.GlobalVars.TAG;
import static org.stratum0.statuswidget.GlobalVars.setStatusUrl;

/**
 * Created by tsuro on 9/1/13.
 */
public class StatusActivity extends Activity implements Button.OnClickListener, SpaceStatusListener {

    private SharedPreferences prefs;
    private SpaceStatus status;
    EditText nameBox;
    ToggleButton openCloseButton;
    Button inheritButton;
    TextView currentStatus;

    /*
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
            openCloseButton.setEnabled(false);
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
            openCloseButton.setEnabled(true);
            if(!nameBox.getText().toString().equals(status.getOpenedBy())) {
                inheritButton.setEnabled(status.isOpen());
            }
            StatusActivity.this.setProgressBarIndeterminate(false);
            StatusActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }
    */

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = SpaceStatus.getInstance();


        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.status_layout);

        openCloseButton = (ToggleButton) findViewById(R.id.openCloseButton);
        inheritButton = (Button) findViewById(R.id.inheritButton);
        nameBox = (EditText) findViewById(R.id.nameBox);
        currentStatus = (TextView) findViewById(R.id.currentStatus);
        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);

        openCloseButton.setOnClickListener(this);
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
        SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(this);
        updateTask.addListener(this);
        updateTask.execute();
    }

    /*
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

        openCloseButton.setChecked(status.isOpen());
        if(!nameBox.getText().toString().equals(status.getOpenedBy())) {
            inheritButton.setEnabled(status.isOpen());
        }
        if(status.isOpen()) {
            currentStatus.setText(status.getSince() + " (" + status.getOpenedBy() + ")");
        } else {
            currentStatus.setText(status.getSince().toString());
        }
    }
    */

    @Override
    protected void onResume() {
        super.onResume();
        //updateActivityInfo();
        SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(this);
        updateTask.addListener(this);
        updateTask.execute();
    }

    @Override
    public void onClick(View view) {

        String new_status;

        boolean b = openCloseButton.isChecked();
        if(b) {
            new_status = "open%20"+nameBox.getText();
        } else {
            new_status = "close";
        }

        SpaceStatusChangeTask changeTask = new SpaceStatusChangeTask(this);
        changeTask.execute(setStatusUrl + new_status);

        //new SuccessCheckTask().execute(b);
    }

    @Override
    public void onPreSpaceStatusUpdate(Context context) {
        openCloseButton.setEnabled(false);
        inheritButton.setEnabled(false);
        StatusActivity.this.setProgressBarIndeterminate(true);
        StatusActivity.this.setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onPostSpaceStatusUpdate(Context context) {
        openCloseButton.setEnabled(true);
        openCloseButton.setChecked(status.isOpen());
        if(!nameBox.getText().toString().equals(status.getOpenedBy())) {
            inheritButton.setEnabled(status.isOpen());
        }

        if(status.isOpen()) {
            SimpleDateFormat isodate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            currentStatus.setText(String.format("%s (%s)", isodate.format(status.getSince().getTime()), status.getOpenedBy()));
        }
        else {
            currentStatus.setText("");
        }

        StatusActivity.this.setProgressBarIndeterminate(false);
        StatusActivity.this.setProgressBarIndeterminateVisibility(false);
    }

}
