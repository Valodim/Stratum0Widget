package org.stratum0.statuswidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;

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
    ProgressBar progressBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = SpaceStatus.getInstance();

        setContentView(R.layout.status_layout);

        openCloseButton = (ToggleButton) findViewById(R.id.openCloseButton);
        inheritButton = (Button) findViewById(R.id.inheritButton);
        nameBox = (EditText) findViewById(R.id.nameBox);
        currentStatus = (TextView) findViewById(R.id.currentStatus);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
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

    @Override
    protected void onResume() {
        super.onResume();
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

    }

    @Override
    public void onPreSpaceStatusUpdate(Context context) {
        openCloseButton.setEnabled(false);
        inheritButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
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

        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }

}
