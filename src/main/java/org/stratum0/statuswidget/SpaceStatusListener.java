package org.stratum0.statuswidget;

import android.content.Context;

/**
 * Created by Matthias Uschok <dev@uschok.de> on 2013-09-29.
 */
public interface SpaceStatusListener {
    public void onPreSpaceStatusUpdate(Context context);
    public void onPostSpaceStatusUpdate(Context context);
    public void onProgressSpaceStatusUpdate(Context context, int progress);
}
