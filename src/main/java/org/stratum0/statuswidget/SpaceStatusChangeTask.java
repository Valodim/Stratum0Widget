package org.stratum0.statuswidget;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.stratum0.statuswidget.GlobalVars.TAG;

/**
 * Created by matthias on 10/6/13.
 */
public class SpaceStatusChangeTask extends AsyncTask <String, Void, Void> {

    private Activity caller;

    public SpaceStatusChangeTask(Activity caller) {
        this.caller = caller;
    }

    @Override
    protected Void doInBackground(String... strings) {

        try {
            URL u = new URL(strings[0]);
            URLConnection c = u.openConnection();
            c.connect();
            c.getContent();
            Thread.sleep(1000);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Update request: malformed URL.", e);
        } catch (IOException e) {
            Log.e(TAG, "Update request: could not connect to server.", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Wait for new status didn't finish:", e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        SpaceStatusUpdateTask updateTask = new SpaceStatusUpdateTask(caller);
        updateTask.addListener((SpaceStatusListener) caller);
        updateTask.execute();
    }
}
