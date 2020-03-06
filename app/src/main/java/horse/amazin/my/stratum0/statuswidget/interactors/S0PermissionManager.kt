package horse.amazin.my.stratum0.statuswidget.interactors

import android.content.Context
import okio.ByteString.Companion.decodeHex
import java.security.MessageDigest

object S0PermissionManager {
    fun maySetSpaceStatus(context: Context): Boolean {
        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        return preferences.getBoolean("wasAtS0Before", false) ||
                preferences.getBoolean("spottedS0Wifi", false) ||
                preferences.getBoolean("allowSetSpaceStatus", false)
    }

    fun allowSetSpaceStatus(context: Context) {
        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        preferences.edit().putBoolean("allowSetSpaceStatus", true).apply()
    }

    fun isS0WifiPwd(pwd: ByteArray): Boolean {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val pwdDigest = messageDigest.digest(pwd)
        return pwdDigest.contentEquals(S0WIFISHA256)
    }

    private val S0WIFISHA256 = "b2f34b2eb4fb32ca1928016d9d502d4e9d88ba0730e2a24eca3df7601be450b6".decodeHex().toByteArray()
}