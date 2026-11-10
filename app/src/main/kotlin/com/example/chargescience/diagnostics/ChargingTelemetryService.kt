package com.example.chargescience.diagnostics

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.chargescience.MainActivity
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Android 14 / 15 / 17 compliant Foreground Service with type specialUse.
 * Keeps CPU active while charging via a partial wake lock to capture
 * deterministic 1 Hz telemetry and presents a silent live notification.
 */
class ChargingTelemetryService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var notificationJob: Job? = null

    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val powerDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                stopTelemetryAndShutdown()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()

        val filter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(powerDisconnectReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopTelemetryAndShutdown()
            return START_NOT_STICKY
        }

        // Start Foreground Service with initial notification
        val notification = buildNotification("Recording Charging Telemetry...", "Initializing sensor pipeline")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )

        acquireChargingWakeLock()
        observeTelemetryForNotification()

        return START_STICKY
    }

    private fun acquireChargingWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ChargeScience:ChargingTelemetryWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12-hour safe cap
            }
        }
    }

    private fun releaseChargingWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun observeTelemetryForNotification() {
        notificationJob?.cancel()
        val collector = BatteryTelemetryCollector(applicationContext)

        notificationJob = serviceScope.launch {
            collector.telemetry.collect { telemetry ->
                if (!telemetry.isCharging) {
                    stopTelemetryAndShutdown()
                    return@collect
                }

                val title = String.format(
                    Locale.US,
                    "Charging: %.1f W (%s)",
                    telemetry.wattage,
                    telemetry.protocolName
                )
                val body = String.format(
                    Locale.US,
                    "%,d mV • %,d mA • %d%% • %.1f°C",
                    telemetry.voltageMv,
                    telemetry.currentMa,
                    telemetry.level,
                    telemetry.temperatureCelsius
                )

                notificationManager.notify(NOTIFICATION_ID, buildNotification(title, body))
            }
        }
    }

    private fun stopTelemetryAndShutdown() {
        notificationJob?.cancel()
        releaseChargingWakeLock()
        try {
            unregisterReceiver(powerDisconnectReceiver)
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseChargingWakeLock()
        try {
            unregisterReceiver(powerDisconnectReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Charging Diagnostics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live wattage and charging telemetry"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "charging_telemetry_channel"
        const val NOTIFICATION_ID = 4001
        const val ACTION_STOP_SERVICE = "com.example.chargescience.ACTION_STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, ChargingTelemetryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChargingTelemetryService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
