package org.stratum0.statuswidget.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.*
import android.support.annotation.ColorRes
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.stratum0.statuswidget.BuildConfig
import org.stratum0.statuswidget.R
import org.stratum0.statuswidget.SpaceStatus
import org.stratum0.statuswidget.SpaceStatusData
import org.stratum0.statuswidget.interactors.SshKeyStorage
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher
import org.stratum0.statuswidget.service.SpaceDoorService
import org.stratum0.statuswidget.service.SpaceStatusService


@SuppressLint("ClickableViewAccessibility")
class StatusActivity : Activity() {
    companion object {
        private val REQUEST_CODE_IMPORT_SSH = 1
        private val PRESS_LONGER_HINT_TIMEOUT = 90
        private val NICK_PATTERN = Regex("[a-zA-Z_\\[\\]{}^`|][a-zA-Z0-9_\\[\\]{}^`|-]+")
    }

    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    private lateinit var prefs: SharedPreferences

    private val viewAnimator: ToolableViewAnimator by bindView(R.id.animator)
    private val buttonOpen: View by bindView(R.id.button_open)
    private val buttonInherit: View by bindView(R.id.button_inherit)
    private val buttonClose: View by bindView(R.id.button_close)
    private val buttonUnlock: View by bindView(R.id.button_unlock)

    private val currentStatusText: TextView by bindView(R.id.current_status_text)
    private val currentStatusTextUnlocked: TextView by bindView(R.id.current_status_text_unlocked)
    private val currentStatusTextLoading: TextView by bindView(R.id.current_status_text_loading)

    private val statusIcon: View by bindView(R.id.set_status_icon)
    private val statusIconBackground: ImageView by bindView(R.id.set_status_icon_background)
    private val statusProgress: View by bindView(R.id.set_status_progress)
    private val statusUnlockedOk: ImageView by bindView(R.id.unlocked_ok_icon)

    private val settingsEditName: EditText by bindView(R.id.settings_edit_name)
    private val settingsSshStatus: TextView by bindView(R.id.settings_ssh_status)
    private val settingsSshImport: View by bindView(R.id.settings_ssh_import)
    private val settingsSshPass: EditText by bindView(R.id.settings_ssh_pass)

    private val textUnlockError: TextView by bindView(R.id.text_unlock_error)

    private lateinit var username: String
    private lateinit var lastStatusData: SpaceStatusData

    private lateinit var sshKeyStorage: SshKeyStorage

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SpaceStatusService.EVENT_UPDATE_RESULT -> {
                    val status = intent.getParcelableExtra<SpaceStatusData>(SpaceStatusService.EXTRA_STATUS)
                    onPostSpaceStatusUpdate(status)
                }
                SpaceDoorService.EVENT_UNLOCK_STATUS -> {
                    val statusOk = intent.getBooleanExtra(SpaceDoorService.EXTRA_STATUS, false)
                    val errorRes = if (!statusOk) {
                         intent.getIntExtra(SpaceDoorService.EXTRA_ERROR_RES, 0)
                    } else {
                        null
                    }
                    onDoorUnlockStatusEvent(errorRes)
                }
            }
        }
    }

    private val onTouchListener = View.OnTouchListener { view, event ->
        val isUnlockButton = view.id == R.id.button_unlock
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startFadeoutAnimation(isUnlockButton)
                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP -> {
                abortFadeoutAnimation()
                view.performClick()
                return@OnTouchListener false
            }
        }
        false
    }

    private var holdingButton = false
    private var triggeredUpdate = false
    private var triggeredUnlock = false

    private var lastButtonDown: Long? = null

    private fun startFadeoutAnimation(isUnlock: Boolean) {
        if (holdingButton || triggeredUpdate || triggeredUnlock) {
            return
        }
        lastButtonDown = SystemClock.elapsedRealtime()

        if (!isUnlock && username.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_nick), Toast.LENGTH_LONG).show()
            return
        }

        holdingButton = true

        val fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_out)

        fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(anim: Animation) {
                if (holdingButton) {
                    holdingButton = false
                    if (isUnlock) {
                        performDoorUnlockOperation()
                    } else {
                        performSpaceStatusOperation()
                    }
                }
            }

            override fun onAnimationRepeat(anim: Animation) {
            }

            override fun onAnimationStart(anim: Animation) {
            }
        })

        currentStatusText.startAnimation(fadeOutAnim)
        statusIcon.startAnimation(fadeOutAnim)

        statusProgress.visibility = View.VISIBLE
    }

    private fun abortFadeoutAnimation() {
        if (holdingButton) {
            holdingButton = false

            currentStatusText.clearAnimation()
            statusIcon.clearAnimation()
            statusProgress.visibility = View.GONE

            val timeSinceButtonDown = SystemClock.elapsedRealtime() - (lastButtonDown?:0L)
            if (timeSinceButtonDown < PRESS_LONGER_HINT_TIMEOUT) {
                Toast.makeText(this, getString(R.string.toast_press_longer), Toast.LENGTH_SHORT).show()
            }
            lastButtonDown = null
        }
    }

    private fun performSpaceStatusOperation() {
        triggeredUpdate = true
        when (lastStatusData.status) {
            SpaceStatus.OPEN -> {
                if (username == lastStatusData.openedBy) {
                    SpaceStatusService.triggerStatusUpdate(applicationContext, null)
                    currentStatusTextLoading.text = getString(R.string.status_progress_closing)
                } else {
                    SpaceStatusService.triggerStatusUpdate(applicationContext, username)
                    currentStatusTextLoading.text = getString(R.string.status_progress_inheriting)
                }
            }
            SpaceStatus.CLOSED -> {
                SpaceStatusService.triggerStatusUpdate(applicationContext, username)
                currentStatusTextLoading.text = getString(R.string.status_progress_opening)
            }
            SpaceStatus.UNKNOWN -> {
                throw IllegalStateException()
            }
        }

        currentStatusTextLoading.visibility = View.VISIBLE
    }

    private fun performDoorUnlockOperation() {
        triggeredUnlock = true

        currentStatusTextLoading.text = getString(R.string.status_progress_unlock)
        currentStatusTextLoading.visibility = View.VISIBLE

        SpaceDoorService.triggerDoorUnlock(applicationContext)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(0, 0)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        setFinishOnTouchOutside(true)
        setContentView(R.layout.status_layout)

        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        sshKeyStorage = SshKeyStorage(applicationContext)

        buttonOpen.setOnTouchListener(onTouchListener)
        buttonInherit.setOnTouchListener(onTouchListener)
        buttonClose.setOnTouchListener(onTouchListener)
        buttonUnlock.setOnTouchListener(onTouchListener)

        findViewById<View>(R.id.button_settings).setOnClickListener { onClickSettings() }
        findViewById<View>(R.id.button_settings_cancel).setOnClickListener { onClickSettingsCancel() }
        findViewById<View>(R.id.button_settings_save).setOnClickListener { onClickSettingsSave() }
        findViewById<View>(R.id.button_error_back).setOnClickListener { onClickBack() }
        findViewById<View>(R.id.button_settings_ssh_save).setOnClickListener { onClickSettingsSshSave() }
        findViewById<View>(R.id.button_settings_ssh_cancel).setOnClickListener { onClickSettingsSshCancel() }

        buttonUnlock.isEnabled = sshKeyStorage.hasKey()

        settingsSshImport.setOnClickListener { onClickSshImport() }

        username = prefs.getString("username", "")

        viewAnimator.displayedChildId = R.id.layout_progress

        refreshStatus()
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(0, 0)
    }

    private fun onClickSshImport() {
        sshKeyStorage.clearKey()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_CODE_IMPORT_SSH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_IMPORT_SSH) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        if (resultCode == RESULT_OK && data != null) {
            val keyData = readSshKeyData(data.data)
            if (keyData != null) {
                if (!sshKeyStorage.looksLikeKey(keyData)) {
                    Toast.makeText(this, "This does not look like a key", Toast.LENGTH_LONG).show()
                } else if (sshKeyStorage.isMatchingPassword(keyData, null)) {
                    sshKeyStorage.setKey(keyData, "")
                } else {
                    displayPassphraseInput(keyData)
                }
            }
        }

        updateSshStatus()
    }

    private var candidateKeyData: String? = null

    private fun displayPassphraseInput(keyData: String) {
        settingsSshPass.setText("")

        candidateKeyData = keyData

        viewAnimator.displayedChildId = R.id.layout_ssh_password
    }

    private fun onClickSettingsSshSave() {
        val passphrase = settingsSshPass.text.toString()

        if (sshKeyStorage.isMatchingPassword(candidateKeyData!!, passphrase)) {
            sshKeyStorage.setKey(candidateKeyData!!, passphrase)
            candidateKeyData = null

            updateSshStatus()
            viewAnimator.displayedChildId = R.id.layout_settings
        } else {
            settingsSshPass.error = getString(R.string.settings_error_bad_password)
        }
    }

    private fun onClickSettingsSshCancel() {
        candidateKeyData = null
        settingsSshPass.setText("")

        updateSshStatus()
        viewAnimator.displayedChildId = R.id.layout_settings
    }

    private fun readSshKeyData(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }

    private fun onClickSettingsSave() {
        val username = settingsEditName.text.toString()
        if (username.length < 3) {
            settingsEditName.error = getString(R.string.settings_error_nick_short)
            return
        }
        if (!NICK_PATTERN.matches(username)) {
            settingsEditName.error = getString(R.string.settings_error_nick_pattern)
            return
        }

        prefs.edit().putString("username", username).apply()
        this.username = username

        hideKeyboard()

        viewAnimator.displayedChildId = R.id.layout_progress
        refreshStatus()
    }

    private fun onClickSettingsCancel() {
        hideKeyboard()
        viewAnimator.displayedChildId = R.id.layout_set_status
    }

    private fun onClickSettings() {
        if (holdingButton || triggeredUpdate) {
            return
        }

        viewAnimator.displayedChildId = R.id.layout_settings
        settingsEditName.setText(username)
        settingsEditName.setSelection(username.length)
        settingsEditName.requestFocus()
        showKeyboard(settingsEditName)

        updateSshStatus()
    }

    private fun onClickBack() {
        displayStatus(false)
    }

    private fun updateSshStatus() {
        if (sshKeyStorage.hasKey()) {
            settingsSshStatus.text = getString(R.string.settings_ssh_status_ok)
        } else {
            settingsSshStatus.text = getString(R.string.settings_ssh_status_unconfigured)
        }

        buttonUnlock.isEnabled = sshKeyStorage.hasKey()
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter()
        filter.addAction(SpaceStatusService.EVENT_UPDATE_RESULT)
        filter.addAction(SpaceDoorService.EVENT_UNLOCK_STATUS)

        registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()

        unregisterReceiver(receiver)
    }

    private fun onDoorUnlockStatusEvent(errorRes: Int?) {
        if (errorRes == null) {
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_in)
            statusUnlockedOk.startAnimation(fadeInAnim)
            currentStatusTextUnlocked.startAnimation(fadeInAnim)
            statusUnlockedOk.visibility = View.VISIBLE
            currentStatusTextUnlocked.visibility = View.VISIBLE

            statusProgress.visibility = View.GONE
            currentStatusTextLoading.visibility = View.GONE

            Handler().postDelayed({
                triggeredUnlock = false
                displayStatus(true)
            }, 1000)
        } else {
            triggeredUnlock = false

            statusIcon.visibility = View.INVISIBLE
            currentStatusText.visibility = View.INVISIBLE

            textUnlockError.text = getText(errorRes)
            viewAnimator.displayedChildId = R.id.layout_unlock_error
        }
    }

    private fun refreshStatus() {
        object : AsyncTask<Void, Void, SpaceStatusData>() {
            override fun doInBackground(vararg p0: Void?): SpaceStatusData {
                return stratum0StatusFetcher.fetch()
            }

            override fun onPostExecute(result: SpaceStatusData) {
                onPostSpaceStatusUpdate(result)
            }
        }.execute()
    }

    fun onPostSpaceStatusUpdate(statusData: SpaceStatusData) {
        if (!prefs.getBoolean("spottedS0Wifi", false) && !BuildConfig.DEBUG) {
            viewAnimator.displayedChildId = R.id.layout_wifi_missing
            return
        }

        lastStatusData = statusData

        val animate = triggeredUpdate
        triggeredUpdate = false

        displayStatus(animate)
    }

    private fun displayStatus(animate: Boolean) {
        hideKeyboard()

        viewAnimator.displayedChildId = R.id.layout_set_status

        statusIcon.visibility = View.VISIBLE
        currentStatusText.visibility = View.VISIBLE

        val statusText: String
        @ColorRes val statusColor: Int
        when (lastStatusData.status) {
            SpaceStatus.UNKNOWN -> {
                buttonClose.visibility = View.GONE
                buttonInherit.visibility = View.GONE
                buttonOpen.visibility = View.GONE

                statusText = getString(R.string.status_unknown)
                statusColor = R.color.status_unknown
            }

            SpaceStatus.CLOSED -> {
                buttonClose.visibility = View.GONE
                buttonInherit.visibility = View.GONE
                buttonOpen.visibility = View.VISIBLE

                statusText = getString(R.string.status_closed)
                statusColor = R.color.status_closed
            }

            SpaceStatus.OPEN -> {
                buttonOpen.visibility = View.GONE

                if (username == lastStatusData.openedBy) {
                    buttonInherit.visibility = View.GONE
                    buttonClose.visibility = View.VISIBLE
                } else {
                    buttonInherit.visibility = View.VISIBLE
                    buttonClose.visibility = View.GONE
                }

                val timestamp = lastStatusData.since!!.time.time
                val readableTime =
                        if (timestamp > System.currentTimeMillis() - DateUtils.MINUTE_IN_MILLIS)
                            getString(R.string.time_just_now)
                        else
                            DateUtils.getRelativeDateTimeString(applicationContext, timestamp,
                                    DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0)
                statusText = getString(R.string.status_open_format, lastStatusData.openedBy, readableTime)
                statusColor = R.color.status_open
            }
        }

        currentStatusText.text = statusText
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            statusIconBackground.setColorFilter(resources.getColor(statusColor, null))
        } else {
            @Suppress("DEPRECATION")
            statusIconBackground.setColorFilter(resources.getColor(statusColor))
        }

        if (animate) {
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_in)
            statusIcon.startAnimation(fadeInAnim)
            currentStatusText.startAnimation(fadeInAnim)
        } else {
            statusIcon.clearAnimation()
            currentStatusText.clearAnimation()
        }

        currentStatusTextLoading.visibility = View.GONE
        statusProgress.visibility = View.GONE

        currentStatusTextUnlocked.clearAnimation()
        statusUnlockedOk.clearAnimation()
        currentStatusTextUnlocked.visibility = View.GONE
        statusUnlockedOk.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInputFromInputMethod(view.windowToken, 0)
    }
}
