package horse.amazin.my.stratum0.statuswidget.interactors

import android.content.Context
import android.location.Location
import android.location.LocationManager
import horse.amazin.my.stratum0.statuswidget.BuildConfig

object LocationInteractor {
    private fun isAtS0RightNow(context: Context): Boolean {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return false
            return lastKnownLocation.distanceTo(S0_LOCATION) < 100.0
        } catch (e: SecurityException) {
            return false
        }
    }

    fun checkIfAtS0(context: Context): Boolean {
        if (isAtS0RightNow(context)) {
            val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
            preferences.edit().putBoolean("wasAtS0Before", true).apply()

            return true
        }
        return false
    }

    fun wasAtS0Before(context: Context): Boolean {
        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        return preferences.getBoolean("wasAtS0Before", false) ||
                preferences.getBoolean("spottedS0Wifi", false)
    }

    private val S0_LOCATION = Location("static").let {
        it.latitude = 52.278750
        it.longitude = 10.521056
        it
    }
}