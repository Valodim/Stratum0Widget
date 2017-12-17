package horse.amazin.my.stratum0.statuswidget.interactors

import android.content.Context
import android.content.SharedPreferences
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair

class SshKeyStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ssh-prefs", Context.MODE_PRIVATE)
    private val jsch = JSch()

    fun hasKey(): Boolean {
        return prefs.contains("ssh-privkey-data")
    }

    fun looksLikeKey(keyData: String): Boolean {
        return try {
            KeyPair.load(jsch, keyData.toByteArray(), ByteArray(0))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isMatchingPassword(keyData: String, password: String?): Boolean {
        val keyPair = KeyPair.load(jsch, keyData.toByteArray(), ByteArray(0))
        return keyPair.decrypt(password)
    }

    fun setKey(keyData: String, passphrase: String) {
        prefs.edit()
                .putString("ssh-privkey-data", keyData)
                .putString("ssh-privkey-pass", passphrase)
                .apply()
    }

    fun clearKey() {
        prefs.edit()
                .remove("ssh-privkey-data")
                .remove("ssh-privkey-pass")
                .apply()
    }

    fun getKey(): String {
        return prefs.getString("ssh-privkey-data", null)
    }

    fun getPassword(): String {
        return prefs.getString("ssh-privkey-pass", null)
    }

    fun isKeyOk(): Boolean {
        return isMatchingPassword(getKey(), getPassword())
    }
}