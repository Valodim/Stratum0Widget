package org.stratum0.statuswidget

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.widget.Button

class FirstRunActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(0, 0)

        setFinishOnTouchOutside(true)
        setContentView(R.layout.first_run)

        findViewById<View>(R.id.button_dismiss).setOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(0, 0)
    }
}