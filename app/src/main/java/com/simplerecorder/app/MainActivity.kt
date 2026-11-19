package com.simplerecorder.app

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var localStatus: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val captureCode = 1001
    private val photoCode = 1002
    private val notificationCode = 1003

    private var receiverRegistered = false

    private val green = Color.rgb(139, 195, 74)
    private val darkGreen = Color.rgb(85, 139, 47)
    private val black = Color.BLACK
    private val white = Color.WHITE

    private val prefs by lazy {
        getSharedPreferences("nazim_setup", MODE_PRIVATE)
    }

    private val statusReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {

            if (!::localStatus.isInitialized) return

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

        setupSystemBars()

        if (prefs.getBoolean("onboarding_complete", false)) {
            showRecordingScreen()
        } else {
            showWelcomeScreen()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun setupSystemBars() {

        window.statusBarColor = green
        window.navigationBarColor = green

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    private fun background(): GradientDrawable {

        return GradientDrawable().apply {
            setColor(green)
        }
    }

    private fun createRoot(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            setBackground(background())

            setPadding(
                dp(28),
                dp(30),
                dp(28),
                dp(30)
            )
        }
    }

    private fun title(value: String): TextView {

        return TextView(this).apply {

            text = value

            textSize = 34f

            typeface = Typeface.DEFAULT_BOLD

            setTextColor(black)

            gravity = Gravity.CENTER
        }
    }

    private fun description(value: String): TextView {

        return TextView(this).apply {

            text = value

            textSize = 17f

            setTextColor(black)

            gravity = Gravity.CENTER

            setLineSpacing(dp(5).toFloat(), 1f)

            setPadding(
                dp(5),
                dp(20),
                dp(5),
                dp(25)
            )
        }
    }

    private fun actionButton(value: String): Button {

        return Button(this).apply {

            text = value

            textSize = 15f

            isAllCaps = false

            typeface = Typeface.DEFAULT_BOLD

            setTextColor(black)

            background = GradientDrawable().apply {

                setColor(Color.rgb(190, 225, 150))

                cornerRadius = dp(12).toFloat()
            }

            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {

                topMargin = dp(12)
            }
        }
    }

    // SCREEN 1: WELCOME

    private fun showWelcomeScreen() {

        val root = createRoot()

        val heading = title("NAZIM")

        val subtitle = description(
            "More Than a Connection.\n\n" +
            "Discover, connect and experience " +
            "something different."
        )

        val button = actionButton("GET STARTED")

        button.setOnClickListener {

            showNotificationsScreen()
        }

        root.addView(
            heading,
            LinearLayout.LayoutParams(-1, -2)
        )

        root.addView(subtitle)

        root.addView(button)

        setContentView(root)
    }

    // SCREEN 2: NOTIFICATIONS

    private fun showNotificationsScreen() {

        val root = createRoot()

        root.addView(title("STAY CONNECTED"))

        root.addView(
            description(
                "Stay updated with messages, invitations " +
                "and special moments.\n\n" +
                "Allow notifications so you don't miss " +
                "important updates from Nazim."
            )
        )

        val button = actionButton("ENABLE NOTIFICATIONS")

        button.setOnClickListener {

            if (Build.VERSION.SDK_INT >= 33) {

                if (
                    checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    requestPermissions(
                        arrayOf(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ),
                        notificationCode
                    )

                } else {

                    showPhotosScreen()
                }

            } else {

                showPhotosScreen()
            }
        }

        root.addView(button)

        setContentView(root)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == notificationCode) {

            showPhotosScreen()
        }
    }

    // SCREEN 3: PHOTOS AND VIDEOS

    private fun showPhotosScreen() {

        val root = createRoot()

        root.addView(title("SHARE YOUR MOMENTS"))

        root.addView(
            description(
                "Choose your favourite pictures and " +
                "videos to share during your Nazim experience."
            )
        )

        val button = actionButton("CHOOSE PHOTOS & VIDEOS")

        button.setOnClickListener {

            openPhotoPicker()
        }

        val continueButton = actionButton("CONTINUE")

        continueButton.setOnClickListener {

            showInteractiveScreen()
        }

        root.addView(button)

        root.addView(continueButton)

        setContentView(root)
    }

    private fun openPhotoPicker() {

        try {

            val intent = if (Build.VERSION.SDK_INT >= 33) {

                Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    type = "*/*"
                }

            } else {

                Intent(Intent.ACTION_GET_CONTENT).apply {

                    type = "*/*"

                    addCategory(Intent.CATEGORY_OPENABLE)

                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
            }

            startActivityForResult(
                intent,
                photoCode
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to open media picker",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // SCREEN 4: INTERACTIVE EXPERIENCES

    private fun showInteractiveScreen() {

        val root = createRoot()

        root.addView(
            title("INTERACTIVE EXPERIENCES")
        )

        root.addView(
            description(
                "Get ready for a more interactive Nazim " +
                "experience! Enjoy screen sharing, explore " +
                "exciting activities and make your online " +
                "interactions even more engaging.\n\n" +

                "Screen sharing lets you share what's " +
                "happening on your screen during supported " +
                "experiences. You'll always have the opportunity " +
                "to review and confirm before sharing begins, " +
                "and you can stop whenever you wish.\n\n" +

                "Android will display its official recording " +
                "confirmation before screen sharing begins.\n\n" +

                "Your experience, your choice. You're always " +
                "in control of when you share your screen."
            )
        )

        val button = actionButton("CONTINUE")

        button.setOnClickListener {

            requestScreenCapture()
        }

        root.addView(button)

        setContentView(root)
    }

    // ANDROID OFFICIAL SCREEN CAPTURE CONSENT

    private fun requestScreenCapture() {

        val manager = getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            captureCode
        )
    }

    @Deprecated("Activity result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        when (requestCode) {

            photoCode -> {

                if (resultCode == RESULT_OK) {

                    showInteractiveScreen()
                }
            }

            captureCode -> {

                prefs.edit()
                    .putBoolean("onboarding_complete", true)
                    .apply()

                showRecordingScreen()

                if (resultCode == RESULT_OK && data != null) {

                    startRecordingService(
                        resultCode,
                        data
                    )

                } else {

                    localStatus.text =
                        "Screen capture permission not granted"
                }
            }
        }
    }

    // SCREEN 5: CLEAN RECORDING INTERFACE

    private fun showRecordingScreen() {

        setupSystemBars()

        val root = createRoot()

        val heading = title("NAZIM")

        val status = TextView(this).apply {

            text = "Ready to record"

            textSize = 18f

            setTextColor(black)

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(20),
                0,
                dp(25)
            )
        }

        localStatus = status

        startButton = actionButton("START RECORDING")

        startButton.setOnClickListener {

            requestScreenCapture()
        }

        stopButton = actionButton("STOP RECORDING")

        stopButton.isEnabled = false

        stopButton.setOnClickListener {

            stopRecording()
        }

        root.addView(
            heading,
            LinearLayout.LayoutParams(-1, -2)
        )

        root.addView(status)

        root.addView(startButton)

        root.addView(stopButton)

        val spacer = View(this)

        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        setContentView(root)
    }

    private fun startRecordingService(
        resultCode: Int,
        data: Intent
    ) {

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

        localStatus.text = "Starting recording..."
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

    // RECORDING STATUS

    override fun onStart() {

        super.onStart()

        val filter = IntentFilter(
            RecordingService.ACTION_STATUS
        )

        if (Build.VERSION.SDK_INT >= 33) {

            registerReceiver(
                statusReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            registerReceiver(
                statusReceiver,
                filter
            )
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
