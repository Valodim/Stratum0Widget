package org.stratum0.statuswidget.push

import android.content.Context

object Stratum0StatusUpdater {
    fun initializeBackgroundUpdates(context: Context) {
        SpaceUpdateJobService.jobSchedulePeriodicRefresh(context)
    }

    fun stopBackgroundUpdates(context: Context) {
        SpaceUpdateJobService.jobCancelPeriodicRefresh(context)
    }

    fun hasPush(context: Context): Boolean {
        return false
    }
}