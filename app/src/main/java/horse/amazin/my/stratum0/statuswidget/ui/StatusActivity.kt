package horse.amazin.my.stratum0.statuswidget.ui


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorRes
import horse.amazin.my.stratum0.statuswidget.R
import horse.amazin.my.stratum0.statuswidget.SpaceLocation
import horse.amazin.my.stratum0.statuswidget.SpaceStatus
import horse.amazin.my.stratum0.statuswidget.SpaceStatusData
import horse.amazin.my.stratum0.statuswidget.databinding.StatusLayoutBinding
import horse.amazin.my.stratum0.statuswidget.interactors.S0PermissionManager
import horse.amazin.my.stratum0.statuswidget.interactors.SshKeyStorage
import horse.amazin.my.stratum0.statuswidget.interactors.StatusFetcher
import horse.amazin.my.stratum0.statuswidget.service.DoorUnlockService
import horse.amazin.my.stratum0.statuswidget.service.StatusChangerService
import horse.amazin.my.stratum0.statuswidget.service.Stratum0WidgetProvider
import java.lang.ref.WeakReference


class StatusActivity : Activity() {
    companion object {
        private val REQUEST_CODE_IMPORT_SSH = 1
        private val PRESS_LONGER_HINT_TIMEOUT = 140
        private val NICK_PATTERN = Regex("[a-zA-Z_\\[\\]{}^`|][a-zA-Z0-9_\\[\\]{}^`|-]+")
    }

    private val stratum0StatusFetcher = StatusFetcher()
    private lateinit var prefs: SharedPreferences
    private lateinit var username: String
    private lateinit var lastStatusData: SpaceStatusData
    private lateinit var sshKeyStorage: SshKeyStorage
    private lateinit var binding: StatusLayoutBinding
    private var unlockAction: Boolean = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                StatusChangerService.EVENT_UPDATE_RESULT -> {
                    val status = intent.getParcelableExtra<SpaceStatusData>(StatusChangerService.EXTRA_STATUS)!!
                    onPostSpaceStatusUpdate(status)
                }
                DoorUnlockService.EVENT_UNLOCK_STATUS -> {
                    val statusOk = intent.getBooleanExtra(DoorUnlockService.EXTRA_STATUS, false)
                    val errorRes = if (!statusOk) {
                         intent.getIntExtra(DoorUnlockService.EXTRA_ERROR_RES, 0)
                    } else {
                        null
                    }
                    onDoorUnlockStatusEvent(errorRes)
                }
            }
        }
    }

    private val onTouchListener = View.OnTouchListener { view, event ->
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requireBeenInSpaceOrStartFadeout(view.id)
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

    private fun requireBeenInSpaceOrStartFadeout(pressedButtonId: Int) {
        if (!S0PermissionManager.maySetSpaceStatus(applicationContext)) {
            binding.animator.displayedChildId = R.id.layout_never_in_space
        } else {
            val action = when (pressedButtonId) {
                R.id.button_space2, R.id.button_space3 -> if (unlockAction) {
                    ButtonActionType.UNLOCK
                } else {
                    ButtonActionType.LOCK
                }
                R.id.buttonUnlock -> ButtonActionType.UNLOCK
                R.id.buttonLock-> ButtonActionType.LOCK
                R.id.buttonClose -> ButtonActionType.CLOSE
                R.id.buttonOpen -> ButtonActionType.OPEN
                R.id.buttonInherit -> ButtonActionType.INHERIT
                else -> throw java.lang.IllegalArgumentException("Unknown button!")
            }
            val location = when (pressedButtonId) {
                R.id.button_space2 -> SpaceLocation.Space2
                R.id.button_space3 -> SpaceLocation.Space3
                else -> null
            }
            startFadeoutAnimation(action, location)
        }
    }

    private fun onClickIamInSpace() {
        if (S0PermissionManager.isS0WifiPwd(binding.textPwd.text.toString().toByteArray())) {
            S0PermissionManager.allowSetSpaceStatus(applicationContext)
            Toast.makeText(this, R.string.toast_pwd_ok, Toast.LENGTH_SHORT).show()
            binding.animator.displayedChildId = R.id.layout_set_status
        } else {
            Toast.makeText(this, R.string.toast_pwd_bad, Toast.LENGTH_SHORT).show()
        }
    }

    private var holdingButton = false
    private var triggeredUpdate = false
    private var triggeredDoorOperation = false

    private var lastButtonDown: Long? = null

    enum class ButtonActionType {
        LOCK, UNLOCK, OPEN, CLOSE, INHERIT;

        val isDoorAction
            get() = this == LOCK || this == UNLOCK
    }

    private fun startFadeoutAnimation(actionType: ButtonActionType, location: SpaceLocation? = null) {
        if (holdingButton || triggeredUpdate || triggeredDoorOperation) {
            return
        }
        lastButtonDown = SystemClock.elapsedRealtime()

        if (actionType.isDoorAction && username.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_nick), Toast.LENGTH_LONG).show()
            return
        }

        holdingButton = true

        val fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_out)

        fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(anim: Animation) {
                if (holdingButton) {
                    holdingButton = false
                    when (actionType) {
                        ButtonActionType.LOCK -> if (location != null) {
                            performDoorLockOperation(location)
                        } else {
                            resetSelectLocationView()
                            displayStatus(true)
                        }
                        ButtonActionType.UNLOCK -> if (location != null) {
                            performDoorUnlockOperation(location)
                        } else {
                            resetSelectLocationView()
                            displayStatus(true)
                        }
                        ButtonActionType.CLOSE -> performSpaceStatusOperation(true)
                        ButtonActionType.INHERIT, ButtonActionType.OPEN -> performSpaceStatusOperation(false)
                    }
                }
            }

            override fun onAnimationRepeat(anim: Animation) {
            }

            override fun onAnimationStart(anim: Animation) {
            }
        })

        when (actionType) {
            ButtonActionType.LOCK, ButtonActionType.UNLOCK -> {
                binding.textSelectLocation.startAnimation(fadeOutAnim)
                binding.statusUnlockProgress.visibility = View.VISIBLE
            }
            ButtonActionType.OPEN, ButtonActionType.CLOSE, ButtonActionType.INHERIT -> {
                binding.currentStatusText.startAnimation(fadeOutAnim)
                binding.statusIcon.startAnimation(fadeOutAnim)
                binding.statusProgress.visibility = View.VISIBLE
            }
        }
    }

    private fun performLocationSelection() {
        binding.animator.displayedChildId = R.id.layout_select_location
    }

    private fun abortFadeoutAnimation() {
        if (holdingButton) {
            holdingButton = false

            resetSelectLocationView()
            binding.currentStatusText.clearAnimation()
            binding.statusIcon.clearAnimation()
            binding.statusProgress.visibility = View.GONE

            val timeSinceButtonDown = SystemClock.elapsedRealtime() - (lastButtonDown?:0L)
            if (timeSinceButtonDown < PRESS_LONGER_HINT_TIMEOUT) {
                Toast.makeText(this, getString(R.string.toast_press_longer), Toast.LENGTH_SHORT).show()
            }
            lastButtonDown = null
        }
    }

    private fun performSpaceStatusOperation(isCloseElseInherit: Boolean) {
        triggeredUpdate = true
        when (lastStatusData.status) {
            SpaceStatus.OPEN -> {
                if (isCloseElseInherit) {
                    StatusChangerService.triggerStatusUpdate(applicationContext, null)
                    binding.currentStatusTextLoading.text =
                        getString(R.string.status_progress_closing)
                } else {
                    StatusChangerService.triggerStatusUpdate(applicationContext, username)
                    binding.currentStatusTextLoading.text =
                        getString(R.string.status_progress_inheriting)
                }
            }

            SpaceStatus.CLOSED -> {
                StatusChangerService.triggerStatusUpdate(applicationContext, username)
                binding.currentStatusTextLoading.text = getString(R.string.status_progress_opening)
            }

            SpaceStatus.UPDATING,
            SpaceStatus.ERROR -> {
                throw IllegalStateException()
            }
        }

        binding.currentStatusTextLoading.visibility = View.VISIBLE
    }

    private fun performDoorLockOperation(location: SpaceLocation) {
        triggeredDoorOperation = true

        binding.currentStatusTextLoading.text = getString(R.string.status_progress_lock)
        binding.currentStatusTextLoading.visibility = View.VISIBLE

        DoorUnlockService.triggerDoorLock(applicationContext, location)
    }

    private fun performDoorUnlockOperation(location: SpaceLocation) {
        triggeredDoorOperation = true

        binding.currentStatusTextLoading.text = getString(R.string.status_progress_unlock)
        binding.currentStatusTextLoading.visibility = View.VISIBLE

        DoorUnlockService.triggerDoorUnlock(applicationContext, location)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StatusLayoutBinding.inflate(layoutInflater)

        overridePendingTransition(0, 0)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        setFinishOnTouchOutside(true)

        val view = binding.root
        setContentView(view)

        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        sshKeyStorage = SshKeyStorage(applicationContext)

        with(binding) {
            buttonOpen.setOnTouchListener(onTouchListener)
            buttonInherit.setOnTouchListener(onTouchListener)
            buttonClose.setOnTouchListener(onTouchListener)
            buttonUnlock.setOnClickListener {
                unlockAction = true
                performLocationSelection()
            }
            buttonLock.setOnClickListener {
                unlockAction = false
                performLocationSelection()
            }
            buttonRefresh.setOnClickListener { onClickRefresh() }
            buttonIAmInSpace.setOnClickListener { onClickIamInSpace() }
            buttonSettings.setOnClickListener { onClickSettings() }
            buttonSettingsCancel.setOnClickListener { onClickSettingsCancel() }
            buttonSettingsSave.setOnClickListener { onClickSettingsSave() }
            buttonErrorBack.setOnClickListener { onClickBack() }
            buttonPermissionBack.setOnClickListener { onClickBack() }
            buttonSettingsSshSave.setOnClickListener { onClickSettingsSshSave() }
            buttonSettingsSshCancel.setOnClickListener { onClickSettingsSshCancel() }
            buttonUnlock.isEnabled = sshKeyStorage.hasKey()
            buttonLock.isEnabled = sshKeyStorage.hasKey()
            settingsSshImport.setOnClickListener { onClickSshImport() }
            buttonSpace2.setOnTouchListener(onTouchListener)
            buttonSpace3.setOnTouchListener(onTouchListener)
            buttonSelectLocationBack.setOnClickListener {
                onButtonSelectLocationBack()
            }
        }
        username = prefs.getString("username", "")!!

        binding.animator.displayedChildId = R.id.layout_progress

        refreshStatus()
    }

    private fun onButtonSelectLocationBack() {
        displayStatus(false)
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(0, 0)
    }

    private fun onClickSshImport() {
        sshKeyStorage.clearKey()
        updateSshStatus()

        val foundKeyInClipboard = trySshKeyFromClipboard()

        if (!foundKeyInClipboard) {
            openSshKeyFromFile()
        }
    }

    private fun openSshKeyFromFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_CODE_IMPORT_SSH)
    }

    private fun trySshKeyFromClipboard(): Boolean {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip() || clipboardManager.primaryClip?.itemCount == 0) {
            return false
        }

        val clipboardText = clipboardManager.primaryClip?.getItemAt(0)?.coerceToText(this).toString()
        if (!sshKeyStorage.looksLikeKey(clipboardText)) {
            return false
        }

        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))

        Toast.makeText(this, R.string.key_from_clipboard, Toast.LENGTH_LONG).show()
        askPassphraseAndStoreKey(clipboardText)

        return true
    }

    private fun askPassphraseAndStoreKey(keyData: String) {
        if (sshKeyStorage.isMatchingPassword(keyData, null)) {
            sshKeyStorage.setKey(keyData, "")
        } else {
            displayPassphraseInput(keyData)
        }

        updateSshStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_IMPORT_SSH) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        if (resultCode == RESULT_OK && data != null) {
            val keyData = data.data?.let { readSshKeyData(it) }
            if (keyData != null) {
                if (sshKeyStorage.looksLikeKey(keyData)) {
                    askPassphraseAndStoreKey(keyData)
                } else {
                    Toast.makeText(this, "This does not look like a key", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var candidateKeyData: String? = null

    private fun displayPassphraseInput(keyData: String) {
        binding.settingsSshPass.setText("")

        candidateKeyData = keyData

        binding.animator.displayedChildId = R.id.layout_ssh_password
    }

    private fun onClickSettingsSshSave() {
        val passphrase = binding.settingsSshPass.text.toString()

        if (sshKeyStorage.isMatchingPassword(candidateKeyData!!, passphrase)) {
            sshKeyStorage.setKey(candidateKeyData!!, passphrase)
            candidateKeyData = null

            updateSshStatus()
            binding.animator.displayedChildId = R.id.layout_settings
        } else {
            binding.settingsSshPass.error = getString(R.string.settings_error_bad_password)
        }
    }

    private fun onClickSettingsSshCancel() {
        candidateKeyData = null
        binding.settingsSshPass.setText("")

        updateSshStatus()
        binding.animator.displayedChildId = R.id.layout_settings
    }

    private fun readSshKeyData(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }

    private fun onClickSettingsSave() {
        val username = binding.settingsEditName.text.toString()
        if (username.length < 3) {
            binding.settingsEditName.error = getString(R.string.settings_error_nick_short)
            return
        }
        if (!NICK_PATTERN.matches(username)) {
            binding.settingsEditName.error = getString(R.string.settings_error_nick_pattern)
            return
        }

        prefs.edit().putString("username", username).apply()
        this.username = username

        hideKeyboard()

        binding.animator.displayedChildId = R.id.layout_progress
        refreshStatus()
    }

    private fun onClickSettingsCancel() {
        hideKeyboard()
        binding.animator.displayedChildId = R.id.layout_set_status
    }

    private fun onClickSettings() {
        if (holdingButton || triggeredUpdate) {
            return
        }

        binding.animator.displayedChildId = R.id.layout_settings
        binding.settingsEditName.setText(username)
        binding.settingsEditName.setSelection(username.length)
        binding.settingsEditName.requestFocus()

        showKeyboard(binding.settingsEditName)

        updateSshStatus()
    }

    private fun onClickBack() {
        displayStatus(false)
    }

    private fun updateSshStatus() {
        if (sshKeyStorage.hasKey()) {
            binding.settingsSshStatus.text = getString(R.string.settings_ssh_status_ok)
        } else {
            binding.settingsSshStatus.text = getString(R.string.settings_ssh_status_unconfigured)
        }

        binding.buttonUnlock.isEnabled = sshKeyStorage.hasKey()
        binding.buttonLock.isEnabled = sshKeyStorage.hasKey()
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter()
        filter.addAction(StatusChangerService.EVENT_UPDATE_RESULT)
        filter.addAction(DoorUnlockService.EVENT_UNLOCK_STATUS)

        registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()

        unregisterReceiver(receiver)
    }

    private fun onDoorUnlockStatusEvent(errorRes: Int?) {
        resetSelectLocationView()
        if (errorRes == null) {
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_in)

            binding.unlockedOkIcon.startAnimation(fadeInAnim)
            binding.currentStatusTextUnlocked.startAnimation(fadeInAnim)
            binding.unlockedOkIcon.visibility = View.VISIBLE
            binding.currentStatusTextUnlocked.visibility = View.VISIBLE

            binding.statusProgress.visibility = View.GONE
            binding.currentStatusTextLoading.visibility = View.GONE

            Handler().postDelayed({
                triggeredDoorOperation = false
                displayStatus(true)
            }, 1000)
        } else {
            triggeredDoorOperation = false

            binding.statusIcon.visibility = View.INVISIBLE
            binding.currentStatusText.visibility = View.INVISIBLE

            binding.textUnlockError.text = getText(errorRes)
            binding.animator.displayedChildId = R.id.layout_unlock_error
        }
    }

    private fun resetSelectLocationView() {
        binding.statusUnlockProgress.visibility = View.GONE
        binding.textSelectLocation.clearAnimation()
        binding.textSelectLocation.visibility = View.VISIBLE
    }

    private fun onClickRefresh() {
        binding.animator.displayedChildId = R.id.layout_progress
        refreshStatus()
    }

    private class RefreshTask(context: StatusActivity) : AsyncTask<Void, Void, SpaceStatusData>() {

        private val activityReference: WeakReference<StatusActivity> = WeakReference(context)

        override fun doInBackground(vararg p0: Void?): SpaceStatusData {
            return activityReference.get()?.stratum0StatusFetcher?.fetch(700)!!
        }

        override fun onPostExecute(result: SpaceStatusData) {
            activityReference.get()?.onPostSpaceStatusUpdate(result)
        }
    }

    private fun refreshStatus() {

        RefreshTask(this).execute()
    }

    fun onPostSpaceStatusUpdate(statusData: SpaceStatusData) {
        val isErrorStatus = statusData.status == SpaceStatus.ERROR
        if (!isErrorStatus) {
            Stratum0WidgetProvider.sendRefreshBroadcast(applicationContext, statusData)
        }

        lastStatusData = statusData

        val animate = triggeredUpdate
        triggeredUpdate = false

        displayStatus(animate)
    }

    private fun displayStatus(animate: Boolean) {
        hideKeyboard()

        binding.animator.displayedChildId = R.id.layout_set_status

        binding.statusIcon.visibility = View.VISIBLE
        binding.currentStatusText.visibility = View.VISIBLE

        val statusText: String
        @ColorRes val statusColor: Int
        when (lastStatusData.status) {
            SpaceStatus.UPDATING,
            SpaceStatus.ERROR -> {
                binding.buttonClose.visibility = View.GONE
                binding.buttonInherit.visibility = View.GONE
                binding.buttonOpen.visibility = View.GONE
                binding.buttonLock.visibility = View.GONE
                binding.buttonUnlock.visibility = View.GONE
                binding.buttonRefresh.visibility = View.VISIBLE

                statusText = getString(R.string.status_error)
                statusColor = R.color.status_unknown
            }

            SpaceStatus.CLOSED -> {
                binding.buttonClose.visibility = View.GONE
                binding.buttonInherit.visibility = View.GONE
                binding.buttonOpen.visibility = View.VISIBLE
                binding.buttonLock.visibility = View.VISIBLE
                binding.buttonUnlock.visibility = View.GONE

                statusText = getString(R.string.status_closed)
                statusColor = R.color.status_closed
            }

            SpaceStatus.OPEN -> {
                binding.buttonOpen.visibility = View.GONE
                binding.buttonLock.visibility = View.GONE
                binding.buttonUnlock.visibility = View.VISIBLE

                if (username == lastStatusData.openedBy) {
                    binding.buttonInherit.visibility = View.GONE
                    binding.buttonClose.visibility = View.VISIBLE
                } else {
                    binding.buttonInherit.visibility = View.VISIBLE
                    binding.buttonClose.visibility = View.VISIBLE
                }

                val timestamp = lastStatusData.since!!.time.time

                val readableTime = getReadableTime(timestamp)
                statusText = getString(R.string.status_open_format, lastStatusData.openedBy, readableTime)
                statusColor = R.color.status_open
            }
        }

        binding.currentStatusText.text = statusText
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.statusIconBackground.setColorFilter(resources.getColor(statusColor, null))
        } else {
            @Suppress("DEPRECATION")
            binding.statusIconBackground.setColorFilter(resources.getColor(statusColor))
        }

        if (animate) {
            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_in)
            binding.statusIcon.startAnimation(fadeInAnim)
            binding.currentStatusText.startAnimation(fadeInAnim)
        } else {
            binding.statusIcon.clearAnimation()
            binding.currentStatusText.clearAnimation()
        }

        binding.currentStatusTextLoading.visibility = View.GONE
        binding.statusProgress.visibility = View.GONE

        binding.currentStatusTextUnlocked.clearAnimation()
        binding.unlockedOkIcon.clearAnimation()
        binding.currentStatusTextUnlocked.visibility = View.GONE
        binding.unlockedOkIcon.visibility = View.GONE
    }

    private fun getReadableTime(timestamp: Long): String {
        if (timestamp > System.currentTimeMillis() - DateUtils.MINUTE_IN_MILLIS)
            return getString(R.string.time_just_now)
        else {
            val date = when {
                DateUtils.isToday(timestamp) -> getString(R.string.time_today)
                DateUtils.isToday(timestamp + DateUtils.DAY_IN_MILLIS) -> getString(R.string.time_yesterday)
                timestamp > (System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * 7) ->
                    DateUtils.formatDateTime(this, timestamp, DateUtils.FORMAT_SHOW_WEEKDAY)
                else -> DateUtils.formatDateTime(this, timestamp,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
            }
            val time = DateUtils.formatDateTime(this, timestamp,
                    DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)
            return date + "\u00A0" + time
        }
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
