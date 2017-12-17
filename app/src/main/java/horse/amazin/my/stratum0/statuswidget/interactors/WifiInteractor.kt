package horse.amazin.my.stratum0.statuswidget.interactors

import android.content.Context
import android.net.wifi.WifiManager
import horse.amazin.my.stratum0.statuswidget.BuildConfig

object WifiInteractor {
    private val WIFI_SSID_S0 = "Stratum0"

    fun isOnStratum0Wifi(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return wifiInfo.ssid != null && wifiInfo.ssid.contains(WIFI_SSID_S0, true)
    }

    fun checkWifi(context: Context) {
        val isOnS0Wifi = isOnStratum0Wifi(context)
        if (isOnS0Wifi) {
            val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
            preferences.edit().putBoolean("spottedS0Wifi", true).apply()
        }
    }

    fun hasSeenS0Wifi(context: Context): Boolean {
        if (BuildConfig.DEBUG) {
            return true
        }

        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        return preferences.getBoolean("spottedS0Wifi", false)
    }
}