package org.stratum0.statuswidget

import android.content.Context
import android.content.SharedPreferences

class SshKeyStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ssh-prefs", Context.MODE_PRIVATE)

    fun hasKey(): Boolean {
        return prefs.contains("ssh-privkey-data")
    }

    fun setKey(keyData: String) {
        prefs.edit().putString("ssh-privkey-data", keyData).apply()
    }

    fun clearKey() {
        prefs.edit().remove("ssh-privkey-data").apply()
    }
}