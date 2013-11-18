package org.stratum0.statuswidget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * Created by Matthias Uschok <dev@uschok.de> on 2013-10-09.
 */
public class FirstRunActivity extends Activity implements View.OnClickListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.first_run);

        Button dismissButton = (Button) findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.dismissButton) {
            this.finish();
        }
    }
}