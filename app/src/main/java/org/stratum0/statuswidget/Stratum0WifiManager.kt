package org.stratum0.statuswidget

import android.content.Context
import android.net.wifi.WifiManager

object Stratum0WifiManager {
    val WIFI_SSID_S0 = "Stratum0"

    fun isOnStratum0Wifi(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        return wifiInfo.ssid != null && wifiInfo.ssid.contains(WIFI_SSID_S0, true)
    }
}