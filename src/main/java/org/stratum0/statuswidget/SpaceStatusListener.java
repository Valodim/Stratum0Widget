package org.stratum0.statuswidget;

import android.content.Context;

/**
 * Created by matthias on 9/29/13.
 */
public interface SpaceStatusListener {
    public void onPreSpaceStatusUpdate(Context context);
    public void onPostSpaceStatusUpdate(Context context);
}
