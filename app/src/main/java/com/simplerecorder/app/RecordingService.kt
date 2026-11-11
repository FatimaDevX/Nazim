package com.simplerecorder.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.MediaStore
import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.simplerecorder.START"
        const val ACTION_STOP = "com.simplerecorder.STOP"

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"

        const val ACTION_STATUS = "com.simplerecorder.STATUS"
        const val EXTRA_RECORDING = "recording"
        const val EXTRA_ERROR = "error"

        private const val CHANNEL_ID = "screen_recording"
        private const val NOTIFICATION_ID = 101
    }

    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputUri: Uri? = null
    private var outputDescriptor: ParcelFileDescriptor? = null

    private var isRecording = false
    private var isStopping = false

    private val projectionCallback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                finishRecording(false)
            }
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                if (isRecording || isStopping) {
                    return START_NOT_STICKY
                }

                startForegroundNotification()

                val resultCode = intent.getIntExtra(
                    EXTRA_RESULT_CODE,
                    Activity.RESULT_CANCELED
                )

                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(
                        EXTRA_DATA,
                        Intent::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }

                if (data == null || resultCode != Activity.RESULT_OK) {
                    notifyStatus(false, "Screen permission was not granted")
                    stopSelf()
                    return START_NOT_STICKY
                }

                try {
                    startRecording(resultCode, data)
                } catch (e: Exception) {
                    finishRecording(true, e.message)
                }
            }

            ACTION_STOP -> {
                finishRecording(true)
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Nazim")
            .setContentText("Screen recording is active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording(
        resultCode: Int,
        data: Intent
    ) {

        val manager = getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        projection = manager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics

        val width = (metrics.widthPixels / 2) * 2
        val height = (metrics.heightPixels / 2) * 2
        val density = metrics.densityDpi

        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "Recording_${SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())}.mp4"
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "Movies/SimpleScreenRecorder"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        outputUri = contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IllegalStateException("Cannot create video file")

        outputDescriptor = contentResolver.openFileDescriptor(
            outputUri!!,
            "w"
        ) ?: throw IllegalStateException("Cannot open video file")

        recorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(8_000_000)
            setOutputFile(outputDescriptor!!.fileDescriptor)
            prepare()
        }

        projection?.registerCallback(
            projectionCallback,
            Handler(Looper.getMainLooper())
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "SimpleScreenRecorder",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder?.surface,
            null,
            null
        ) ?: throw IllegalStateException("Cannot create screen display")

        recorder?.start()

        isRecording = true
        notifyStatus(true)
    }

    private fun finishRecording(
        stopProjection: Boolean,
        error: String? = null
    ) {

        if (isStopping) return
        isStopping = true

        var successful = isRecording

        try {
            if (isRecording) {
                try {
                    recorder?.stop()
                } catch (_: Exception) {
                    successful = false
                }
            } else {
                successful = false
            }

            isRecording = false

            try {
                virtualDisplay?.release()
            } catch (_: Exception) {
            }

            virtualDisplay = null

            try {
                recorder?.reset()
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null

            try {
                projection?.unregisterCallback(projectionCallback)
            } catch (_: Exception) {
            }

            if (stopProjection) {
                try {
                    projection?.stop()
                } catch (_: Exception) {
                }
            }

            projection = null

            try {
                outputDescriptor?.close()
            } catch (_: Exception) {
            }

            outputDescriptor = null

            outputUri?.let { uri ->
                if (successful) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }

                    contentResolver.update(uri, values, null, null)
                } else {
                    contentResolver.delete(uri, null, null)
                }
            }

            outputUri = null

        } finally {
            notifyStatus(false, error)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isStopping = false
        }
    }

    private fun notifyStatus(
        recording: Boolean,
        error: String? = null
    ) {
        val intent = Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_RECORDING, recording)
            putExtra(EXTRA_ERROR, error)
        }

        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (!isStopping && (isRecording || projection != null)) {
            finishRecording(true)
        }
        super.onDestroy()
    }
}
