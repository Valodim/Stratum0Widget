package org.stratum0.statuswidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;

/**
 * Created by tsuro on 9/1/13.
 */
public class StatusActivity extends Activity implements Button.OnClickListener, SpaceStatusListener {

    private SharedPreferences prefs;
    private SpaceStatus status;
    EditText nameBox;
    Button openCloseButton;
    Button inheritButton;
    TextView currentStatusText;
    ProgressBar progressBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = SpaceStatus.getInstance();

        setContentView(R.layout.status_layout);

        openCloseButton = (Button) findViewById(R.id.openCloseButton);
        inheritButton = (Button) findViewById(R.id.inheritButton);
        nameBox = (EditText) findViewById(R.id.nameBox);
        currentStatusText = (TextView) findViewById(R.id.currentStatus);
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
                if (status.getStatus() == SpaceStatus.Status.OPEN &&
                    !nameBox.getText().toString().equals(status.getOpenedBy())) {
                    inheritButton.setVisibility(View.VISIBLE);
                }
                else{
                    inheritButton.setVisibility(View.GONE);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", editable.toString());
                editor.commit();
            }
        });

        String username = prefs.getString("username", getString(R.string.editText_defaultName));
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

        String queryString = "/update?";

        switch (view.getId()) {
            case R.id.openCloseButton:
                if (status.getStatus() == SpaceStatus.Status.OPEN) {
                    queryString += "open=false";
                }
                else if (status.getStatus() == SpaceStatus.Status.CLOSED) {
                    queryString += "open=true&by=" + nameBox.getText();
                }
                break;
            case R.id.inheritButton:
                queryString += "open=true&by=" + nameBox.getText();
                break;
        }

        SpaceStatusChangeTask changeTask = new SpaceStatusChangeTask(this);
        changeTask.addListener(this);
        changeTask.execute(queryString);

    }

    @Override
    public void onPreSpaceStatusUpdate(Context context) {
        openCloseButton.setEnabled(false);
        inheritButton.setVisibility(View.GONE);
        currentStatusText.setVisibility(TextView.GONE);
        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    @Override
    public void onPostSpaceStatusUpdate(Context context) {
        if(status.getStatus() == SpaceStatus.Status.UNKNOWN) {
            currentStatusText.setText(getString(R.string.status_unknown));
        }
        else {
            openCloseButton.setEnabled(true);

            if(status.getStatus() == SpaceStatus.Status.OPEN) {
                SimpleDateFormat isodate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                openCloseButton.setText(getString(R.string.button_openClose_open));
                currentStatusText.setText(String.format("%s (%s)", isodate.format(status.getLastChange().getTime()), status.getOpenedBy()));
                if (!nameBox.getText().toString().equals(status.getOpenedBy())) {
                    inheritButton.setVisibility(View.VISIBLE);
                }
            }
            else {
                openCloseButton.setText(getString(R.string.button_openClose_closed));
                currentStatusText.setText(getString(R.string.status_closed));
            }
        }

        currentStatusText.setVisibility(TextView.VISIBLE);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        if (!progressBar.isIndeterminate()) {
            progressBar.setIndeterminate(true);
        }
    }

}
