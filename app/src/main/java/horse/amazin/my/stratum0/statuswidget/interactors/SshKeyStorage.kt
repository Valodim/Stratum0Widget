package horse.amazin.my.stratum0.statuswidget.interactors

import android.content.Context
import android.content.SharedPreferences
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.password.PasswordUtils

class SshKeyStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ssh-prefs", Context.MODE_PRIVATE)
    private val sshClient = SSHClient()

    fun hasKey(): Boolean {
        return prefs.contains("ssh-privkey-data")
    }

    fun looksLikeKey(keyData: String): Boolean {
        return try {
            sshClient.loadKeys(keyData, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isMatchingPassword(keyData: String, password: String?): Boolean {
        return try {
            val passwordFinder = password?.let {
                PasswordUtils.createOneOff(it.toCharArray())
            }
            sshClient.loadKeys(keyData, null, passwordFinder).private
            true
        } catch (e: Exception) {
            false
        }
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
        return prefs.getString("ssh-privkey-data", "")!!
    }

    fun getPassword(): String {
        return prefs.getString("ssh-privkey-pass", "")!!
    }

    fun isKeyOk(): Boolean {
        return isMatchingPassword(getKey(), getPassword())
    }
}