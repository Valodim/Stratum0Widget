package horse.amazin.my.stratum0.statuswidget


import android.app.Application
import net.schmizz.sshj.common.SecurityUtils

import timber.log.Timber
import timber.log.Timber.DebugTree
import java.security.Security


class Stratum0WidgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Security.removeProvider("BC")
        Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 0)

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}
