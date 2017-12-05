package org.stratum0.statuswidget


import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import org.stratum0.statuswidget.widget.ToolableViewAnimator
import java.text.SimpleDateFormat


class StatusActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    internal lateinit var viewAnimator: ToolableViewAnimator
    internal lateinit var buttonOpen: TextView
    internal lateinit var buttonInherit: TextView
    internal lateinit var buttonClose: TextView

    internal lateinit var currentStatusText: TextView

    internal lateinit var statusIcon: ImageView
    internal lateinit var statusProgress: View

    internal lateinit var settingsEditName: EditText

    private lateinit var username: String
    private lateinit var lastStatusData: SpaceStatusData

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SpaceStatusService.EVENT_REFRESH_IN_PROGRESS -> onPreSpaceStatusUpdate()
                SpaceStatusService.EVENT_REFRESH -> {
                    val status = intent.getParcelableExtra<SpaceStatusData>(SpaceStatusService.EXTRA_STATUS)
                    onPostSpaceStatusUpdate(status)
                }
            }
        }
    }

    private val onTouchListener = object : View.OnTouchListener {
        override fun onTouch(view: View?, event: MotionEvent?): Boolean {
            when (event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startFadeoutAnimation()
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
    var triggeredUpdate = true

    private fun startFadeoutAnimation() {
        holdingButton = true

        val fadeOutAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_out)
        val fadeOutIconAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_out)

        fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(anim: Animation) {
                if (holdingButton) {
                    holdingButton = false
                    performSpaceStatusOperation()
                }
            }

            override fun onAnimationRepeat(anim: Animation) {
            }

            override fun onAnimationStart(anim: Animation) {
            }
        })

        currentStatusText.startAnimation(fadeOutAnim)

        statusIcon.startAnimation(fadeOutIconAnim)
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
                } else {
                    SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, username)
                }
            }
            SpaceStatus.CLOSED -> {
                SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, username)
            }
            SpaceStatus.UNKNOWN -> {
                throw IllegalStateException()
            }
        }
    }

    private val appWidgetIds: IntArray
        get() = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.status_layout)

        viewAnimator = findViewById(R.id.animator)

        buttonOpen = findViewById(R.id.button_open)
        buttonInherit = findViewById(R.id.button_inherit)
        buttonClose = findViewById(R.id.button_close)

        currentStatusText = findViewById(R.id.current_status_text)

        statusIcon = findViewById(R.id.set_status_icon)
        statusProgress = findViewById(R.id.set_status_progress)

        settingsEditName = findViewById(R.id.settings_edit_name)

        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        buttonOpen.setOnTouchListener(onTouchListener)
        buttonInherit.setOnTouchListener(onTouchListener)
        buttonClose.setOnTouchListener(onTouchListener)

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

        username = prefs.getString("username", "")
    }

    private fun onClickSettingsSave() {
        username = settingsEditName.text.toString()
        prefs.edit().putString("username", username).apply()

        viewAnimator.displayedChildId = R.id.layout_set_status
    }

    private fun onClickSettingsCancel() {
        viewAnimator.displayedChildId = R.id.layout_set_status
    }

    private fun onClickSettings() {
        settingsEditName.setText(username)
        viewAnimator.displayedChildId = R.id.layout_edit_name
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter()
        filter.addAction(SpaceStatusService.EVENT_REFRESH_IN_PROGRESS)
        filter.addAction(SpaceStatusService.EVENT_REFRESH)

        registerReceiver(receiver, filter)

        SpaceStatusService.triggerStatusRefresh(applicationContext, appWidgetIds, false)
    }

    override fun onStop() {
        super.onStop()

        unregisterReceiver(receiver)
    }

    fun onPreSpaceStatusUpdate() {
        if (triggeredUpdate) {
            return
        }

        viewAnimator.displayedChildId = R.id.layout_progress
    }

    fun onPostSpaceStatusUpdate(statusData: SpaceStatusData) {
        lastStatusData = statusData

        viewAnimator.displayedChildId = R.id.layout_set_status

        when (statusData.status) {
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

                if (username.equals(statusData.openedBy)) {
                    buttonInherit.visibility = View.GONE
                    buttonClose.visibility = View.VISIBLE
                } else {
                    buttonInherit.visibility = View.VISIBLE
                    buttonClose.visibility = View.GONE
                }

                val isodate = SimpleDateFormat("yyyy-MM-dd HH:mm")
                currentStatusText.text = String.format("%s\nat %s", statusData.openedBy, isodate.format(statusData.lastChange!!.time))
                statusIcon.setImageResource(R.drawable.stratum0_open)
            }
        }

        if (triggeredUpdate) {
            triggeredUpdate = false

            val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.holding_fade_in)
            statusIcon.startAnimation(fadeInAnim)
            currentStatusText.startAnimation(fadeInAnim)

            statusProgress.visibility = View.GONE
        }
    }

}
