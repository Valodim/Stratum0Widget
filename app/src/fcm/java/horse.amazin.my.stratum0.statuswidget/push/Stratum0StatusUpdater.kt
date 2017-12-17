@file:Suppress("UNUSED_PARAMETER") // must be compatible with file in other flavors

package horse.amazin.my.stratum0.statuswidget.push

import android.content.Context
import android.content.pm.PackageManager

object Stratum0StatusUpdater {
    fun initializeBackgroundUpdates(context: Context) {
        if (hasPush(context)) {
            PushFcmUpdateService.subscribeToStatusUpdates()
        } else {
            SpaceUpdateJobService.jobSchedulePeriodicRefresh(context)
        }
    }

    fun stopBackgroundUpdates(context: Context) {
        if (hasPush(context)) {
            PushFcmUpdateService.unsubscribeFromStatusUpdates()
        } else {
            SpaceUpdateJobService.jobCancelPeriodicRefresh(context)
        }
    }

    fun hasPush(context: Context): Boolean {
        try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}