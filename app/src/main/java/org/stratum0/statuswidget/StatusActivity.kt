package org.stratum0.statuswidget


import java.text.SimpleDateFormat

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView


class StatusActivity : Activity(), OnClickListener {

    private lateinit var prefs: SharedPreferences

    internal lateinit var nameBox: EditText
    internal lateinit var openCloseButton: Button
    internal lateinit var inheritButton: Button
    internal lateinit var currentStatusText: TextView
    internal lateinit var progressBar: ProgressBar

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

    private val appWidgetIds: IntArray
        get() = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.status_layout)

        openCloseButton = findViewById(R.id.openCloseButton)
        inheritButton = findViewById(R.id.inheritButton)
        nameBox = findViewById(R.id.nameBox)
        currentStatusText = findViewById(R.id.currentStatus)
        progressBar = findViewById(R.id.progressBar)
        prefs = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        openCloseButton.setOnClickListener(this)
        inheritButton.setOnClickListener(this)
/*        nameBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (status!!.status === SpaceStatus.Status.OPEN && nameBox.text.toString() != status!!.openedBy) {
                    inheritButton.visibility = View.VISIBLE
                } else {
                    inheritButton.visibility = View.GONE
                }
                val editor = prefs.edit()
                editor.putString("username", editable.toString())
                editor.commit()
            }
        })
*/

        val username = prefs.getString("username", getString(R.string.editText_defaultName))
        nameBox.setText(username)
    }

    override fun onClick(view: View) {
        val userName = nameBox.text.toString()
        SpaceStatusService.triggerStatusUpdate(applicationContext, appWidgetIds, userName)
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
        openCloseButton.isEnabled = false
        inheritButton.visibility = View.GONE
        currentStatusText.visibility = TextView.GONE
        progressBar.visibility = ProgressBar.VISIBLE
    }

    fun onPostSpaceStatusUpdate(statusData: SpaceStatusData) {
        when (statusData.status) {
            SpaceStatus.UNKNOWN -> {
                currentStatusText.text = getString(R.string.status_unknown)
            }

            SpaceStatus.CLOSED -> {
                openCloseButton.text = getString(R.string.button_openClose_closed)
                currentStatusText.text = getString(R.string.status_closed)
            }

            SpaceStatus.OPEN -> {
                openCloseButton.isEnabled = true

                val isodate = SimpleDateFormat("yyyy-MM-dd HH:mm")
                openCloseButton.text = getString(R.string.button_openClose_open)
                currentStatusText.text = String.format("%s (%s)", isodate.format(statusData.lastChange!!.time), statusData.openedBy)
                if (nameBox.text.toString() != statusData.openedBy) {
                    inheritButton.visibility = View.VISIBLE
                }
            }
        }

        currentStatusText.visibility = TextView.VISIBLE
        progressBar.visibility = ProgressBar.INVISIBLE
        if (!progressBar.isIndeterminate) {
            progressBar.isIndeterminate = true
        }
    }

}
