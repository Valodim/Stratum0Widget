package org.stratum0.statuswidget.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import org.stratum0.statuswidget.R

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