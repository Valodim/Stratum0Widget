package horse.amazin.my.stratum0.statuswidget


import android.app.Application

import timber.log.Timber
import timber.log.Timber.DebugTree


class Stratum0WidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}
