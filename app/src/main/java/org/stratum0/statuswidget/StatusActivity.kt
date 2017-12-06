package org.stratum0.statuswidget


import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import org.stratum0.statuswidget.util.bindView
import org.stratum0.statuswidget.widget.ToolableViewAnimator
import java.text.SimpleDateFormat


class StatusActivity : Activity() {
    val REQUEST_CODE_IMPORT_SSH = 1

    private lateinit var prefs: SharedPreferences

    private val viewAnimator: ToolableViewAnimator by bindView(R.id.animator)
    private val buttonOpen: TextView by bindView(R.id.button_open)
    private val buttonInherit: TextView by bindView(R.id.button_inherit)
    private val buttonClose: TextView by bindView(R.id.button_close)
    private val buttonUnlock: TextView by bindView(R.id.button_unlock)

    private val currentStatusText: TextView by bindView(R.id.current_status_text)
    private val currentStatusTextUnlocked: TextView by bindView(R.id.current_status_text_unlocked)
    private val currentStatusTextLoading: TextView by bindView(R.id.current_status_text_loading)

    private val statusIcon: ImageView by bindView(R.id.set_status_icon)
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
                SpaceStatusService.EVENT_REFRESH -> {
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

    private val onTouchListener = object : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent?): Boolean {
            val isUnlockButton = view.id == R.id.button_unlock
            when (event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startFadeoutAnimation(isUnlockButton)
                    return false
                }
                MotionEvent.ACTION_UP -> {
                    abortFadeoutAnimation()
                    return false
                }
            }
            return false
        }
    }

    var holdingButton = false
    var triggeredUpdate = false
    var triggeredUnlock = false

    private fun startFadeoutAnimation(isUnlock: Boolean) {
        if (holdingButton || triggeredUpdate || triggeredUnlock) {
            return
        }

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
        }
    }

    private fun performSpaceStatusOperation() {
        triggeredUpdate = true
        when (lastStatusData.status) {
            SpaceStatus.OPEN -> {
                if (username.equals(lastStatusData.openedBy)) {
                    SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, null)
                    currentStatusTextLoading.text = getString(R.string.status_progress_closing)
                } else {
                    SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, username)
                    currentStatusTextLoading.text = getString(R.string.status_progress_inheriting)
                }
            }
            SpaceStatus.CLOSED -> {
                SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, username)
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

    private val appWidgetIds: IntArray
        get() = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.status_layout)

        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        sshKeyStorage = SshKeyStorage(applicationContext)

        buttonOpen.setOnTouchListener(onTouchListener)
        buttonInherit.setOnTouchListener(onTouchListener)
        buttonClose.setOnTouchListener(onTouchListener)
        buttonUnlock.setOnTouchListener(onTouchListener)

        findViewById<View>(R.id.button_settings).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickSettings()
            }
        })

        findViewById<View>(R.id.button_settings_cancel).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickSettingsCancel()
            }
        })
        findViewById<View>(R.id.button_settings_save).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickSettingsSave()
            }
        })
        findViewById<View>(R.id.button_error_back).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickBack()
            }
        })
        findViewById<View>(R.id.button_settings_ssh_save).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickSettingsSshSave()
            }
        })
        findViewById<View>(R.id.button_settings_ssh_cancel).setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                onClickSettingsSshCancel()
            }
        })

        buttonUnlock.isEnabled = sshKeyStorage.hasKey()

        settingsSshImport.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                onClickSshImport()
            }
        })

        username = prefs.getString("username", "")

        viewAnimator.displayedChildId = R.id.layout_progress
        SpaceStatusService.triggerStatusRefresh(applicationContext, appWidgetIds, false)
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

    var candidateKeyData: String? = null

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
        username = settingsEditName.text.toString()
        prefs.edit().putString("username", username).apply()

        hideKeyboard()

        viewAnimator.displayedChildId = R.id.layout_progress
        SpaceStatusService.triggerStatusRefresh(applicationContext, appWidgetIds, false)
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
        filter.addAction(SpaceStatusService.EVENT_REFRESH)
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

            Handler().postDelayed(Runnable {
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

        when (lastStatusData.status) {
            SpaceStatus.UNKNOWN -> {
                buttonClose.visibility = View.GONE
                buttonInherit.visibility = View.GONE
                buttonOpen.visibility = View.GONE

                currentStatusText.text = getString(R.string.status_unknown)
                statusIcon.setImageResource(R.drawable.stratum0_unknown)
            }

            SpaceStatus.CLOSED -> {
                buttonClose.visibility = View.GONE
                buttonInherit.visibility = View.GONE
                buttonOpen.visibility = View.VISIBLE

                currentStatusText.text = getString(R.string.status_closed)
                statusIcon.setImageResource(R.drawable.stratum0_closed)
            }

            SpaceStatus.OPEN -> {
                buttonOpen.visibility = View.GONE

                if (username.equals(lastStatusData.openedBy)) {
                    buttonInherit.visibility = View.GONE
                    buttonClose.visibility = View.VISIBLE
                } else {
                    buttonInherit.visibility = View.VISIBLE
                    buttonClose.visibility = View.GONE
                }

                val isodate = SimpleDateFormat("yyyy-MM-dd HH:mm")
                currentStatusText.text = String.format("Open by %s\nat %s", lastStatusData.openedBy, isodate.format(lastStatusData.lastChange!!.time))
                statusIcon.setImageResource(R.drawable.stratum0_open)
            }
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
