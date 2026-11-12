package com.simplerecorder.app

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var localStatus: TextView
    private lateinit var remoteStatus: TextView

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val requestCode = 1001
    private var receiverRegistered = false

    private val isDark: Boolean
        get() = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    private val backgroundColor: Int
        get() = if (isDark) Color.rgb(18, 18, 22)
                else Color.rgb(245, 246, 250)

    private val cardColor: Int
        get() = if (isDark) Color.rgb(35, 35, 43)
                else Color.WHITE

    private val textColor: Int
        get() = if (isDark) Color.WHITE
                else Color.rgb(25, 25, 35)

    private val secondaryColor: Int
        get() = if (isDark) Color.LTGRAY
                else Color.DKGRAY

    private val accentColor = Color.rgb(68, 138, 255)

    private val statusReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            val recording = intent?.getBooleanExtra(
                RecordingService.EXTRA_RECORDING,
                false
            ) ?: false

            val error = intent?.getStringExtra(
                RecordingService.EXTRA_ERROR
            )

            localStatus.text = when {
                recording -> "● Recording is active"
                error != null -> error
                else -> "Ready to record"
            }

            startButton.isEnabled = !recording
            stopButton.isEnabled = recording
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        if (!isDark) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        buildInterface()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun buildInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(22), dp(30), dp(22), dp(20))
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "NAZIM"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Your screen. Your control."
            textSize = 14f
            setTextColor(secondaryColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(30))
        }

        content.addView(title)
        content.addView(subtitle)

        // LOCAL RECORDING

        content.addView(sectionTitle("📹  LOCAL RECORDING"))

        val localCard = createCard()

        localStatus = TextView(this).apply {
            text = "Ready to record"
            textSize = 14f
            setTextColor(secondaryColor)
            setPadding(0, 0, 0, dp(18))
        }

        startButton = createButton("START RECORDING")

        startButton.setOnClickListener {
            startRecording()
        }

        stopButton = createButton("STOP RECORDING").apply {
            isEnabled = false
        }

        stopButton.setOnClickListener {
            stopRecording()
        }

        localCard.addView(localStatus)
        localCard.addView(startButton)
        localCard.addView(stopButton)

        content.addView(
            localCard,
            sectionMargins()
        )

        // REMOTE SCREEN SHARING

        content.addView(
            sectionTitle("🌐  REMOTE SCREEN SHARING")
        )

        val remoteCard = createCard()

        val remoteDescription = TextView(this).apply {
            text = "Share your screen with the Nazim developer dashboard. You remain in control and can stop sharing at any time."
            textSize = 14f
            setTextColor(secondaryColor)
            setPadding(0, 0, 0, dp(18))
        }

        remoteStatus = TextView(this).apply {
            text = "Remote sharing is not configured yet"
            textSize = 14f
            setTextColor(secondaryColor)
            setPadding(0, 0, 0, dp(18))
        }

        val shareButton = createButton("SHARE MY SCREEN")

        shareButton.setOnClickListener {
            showRemoteInformation()
        }

        val stopSharingButton = createButton("STOP SHARING").apply {
            isEnabled = false
        }

        remoteCard.addView(remoteDescription)
        remoteCard.addView(remoteStatus)
        remoteCard.addView(shareButton)
        remoteCard.addView(stopSharingButton)

        content.addView(
            remoteCard,
            sectionMargins()
        )

        val footer = TextView(this).apply {
            text = "NAZIM  •  Secure Screen Sharing"
            textSize = 12f
            setTextColor(secondaryColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(25), 0, dp(15))
        }

        content.addView(footer)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(0, dp(18), 0, dp(14))
        }
    }

    private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = roundedBackground(cardColor)
        }
    }

    private fun createButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBackground(accentColor)

            val params = LinearLayout.LayoutParams(
                -1,
                dp(52)
            )

            params.bottomMargin = dp(10)
            layoutParams = params
        }
    }

    private fun sectionMargins(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            -1,
            -2
        ).apply {
            bottomMargin = dp(15)
        }
    }

    private fun showRemoteInformation() {

        AlertDialog.Builder(this)
            .setTitle("Nazim Remote Sharing")
            .setMessage(
                "Remote screen sharing is coming in a future update.\n\n" +
                "When available, you will be able to explicitly " +
                "approve screen sharing through Android's official " +
                "permission prompt.\n\n" +
                "Nothing is being shared right now."
            )
            .setPositiveButton("GOT IT", null)
            .show()
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
                localStatus.text = "Recording permission denied"
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
}
