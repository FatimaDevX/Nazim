package com.simplerecorder.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val requestCode = 1001
    private var receiverRegistered = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val recording = intent?.getBooleanExtra(
                RecordingService.EXTRA_RECORDING,
                false
            ) ?: false

            val error = intent?.getStringExtra(
                RecordingService.EXTRA_ERROR
            )

            if (recording) {
                status.text = "Recording..."
            } else {
                status.text = error ?: "Ready to record"
            }

            startButton.isEnabled = !recording
            stopButton.isEnabled = recording
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "NAZIM"
            textSize = 23f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Ready to record"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        startButton = Button(this).apply {
            text = "START RECORDING"
            setOnClickListener {
                startRecording()
            }
        }

        stopButton = Button(this).apply {
            text = "STOP RECORDING"
            isEnabled = false
            setOnClickListener {
                stopRecording()
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(stopButton)

        setContentView(layout)
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter(RecordingService.ACTION_STATUS)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                statusReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(statusReceiver, filter)
        }

        receiverRegistered = true
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(statusReceiver)
            receiverRegistered = false
        }

        super.onStop()
    }

    private fun startRecording() {

        val manager = getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            requestCode
        )
    }

    @Deprecated("Deprecated Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode) {

            if (resultCode == RESULT_OK && data != null) {

                val intent = Intent(
                    this,
                    RecordingService::class.java
                ).apply {
                    action = RecordingService.ACTION_START

                    putExtra(
                        RecordingService.EXTRA_RESULT_CODE,
                        resultCode
                    )

                    putExtra(
                        RecordingService.EXTRA_DATA,
                        data
                    )
                }

                startForegroundService(intent)

            } else {
                status.text = "Recording permission denied"
            }
        }
    }

    private fun stopRecording() {

        val intent = Intent(
            this,
            RecordingService::class.java
        ).apply {
            action = RecordingService.ACTION_STOP
        }

        startService(intent)
    }
}
