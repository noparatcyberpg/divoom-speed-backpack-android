package com.divoom.navigationarrow.navigation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.divoom.navigationarrow.MainActivity
import com.divoom.navigationarrow.R
import kotlin.math.roundToInt

/**
 * Keeps the Bluetooth controller alive and reads motion speed only to pace animation.
 * Speed is never shown or stored.
 */
class NavigationForegroundService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var previousLocation: Location? = null
    private var smoothedSpeedKmh: Float? = null
    private var lastFixAt = 0L
    private var lastGpsFixAt = 0L
    private var stationarySince = 0L

    private val staleFixWatchdog = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (lastFixAt > 0L && now - lastFixAt > FIX_STALE_AFTER_MS) {
                previousLocation = null
                smoothedSpeedKmh = null
                lastFixAt = 0L
                lastGpsFixAt = 0L
                stationarySince = 0L
                MotionStateBus.publish(MotionState(updatedAt = now))
            }
            mainHandler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            serviceType
        )
        startMotionTracking()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMotionTracking() {
        runCatching { locationManager.removeUpdates(this) }
        if (!hasLocationPermission()) {
            MotionStateBus.publish(MotionState(updatedAt = System.currentTimeMillis()))
            stopSelf()
            return
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (providers.isEmpty()) {
            MotionStateBus.publish(MotionState(updatedAt = System.currentTimeMillis()))
            return
        }

        try {
            providers.forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_INTERVAL_MS,
                    MIN_DISTANCE_METERS,
                    this,
                    Looper.getMainLooper()
                )
            }
            mainHandler.removeCallbacks(staleFixWatchdog)
            mainHandler.postDelayed(staleFixWatchdog, WATCHDOG_INTERVAL_MS)
        } catch (_: SecurityException) {
            MotionStateBus.publish(MotionState(updatedAt = System.currentTimeMillis()))
            stopSelf()
        }
    }

    override fun onLocationChanged(location: Location) {
        val now = System.currentTimeMillis()
        if (
            location.provider != LocationManager.GPS_PROVIDER &&
            lastGpsFixAt > 0L &&
            now - lastGpsFixAt < PREFER_GPS_FOR_MS
        ) {
            return
        }
        if (location.provider == LocationManager.GPS_PROVIDER) {
            lastGpsFixAt = now
        }

        val rawMetersPerSecond = when {
            location.hasSpeed() && location.speed >= 0f -> location.speed
            else -> calculateSpeedFrom(previousLocation, location)
        }
        previousLocation = location
        if (rawMetersPerSecond == null || !rawMetersPerSecond.isFinite()) return

        var rawKmh = (rawMetersPerSecond * METERS_PER_SECOND_TO_KMH)
            .coerceIn(0f, MAX_SPEED_KMH.toFloat())
        if (rawKmh < STATIONARY_NOISE_KMH) rawKmh = 0f
        val smoothed = smoothedSpeedKmh
            ?.let { (it * 0.6f) + (rawKmh * 0.4f) }
            ?: rawKmh
        smoothedSpeedKmh = smoothed
        lastFixAt = now

        if (smoothed < STOPPED_THRESHOLD_KMH) {
            if (stationarySince == 0L) stationarySince = now
        } else {
            stationarySince = 0L
        }
        val isStopped =
            stationarySince > 0L && now - stationarySince >= STOPPED_CONFIRMATION_MS

        MotionStateBus.publish(
            MotionState(
                speedKmh = smoothed.roundToInt().coerceIn(0, MAX_SPEED_KMH),
                hasFix = true,
                isStopped = isStopped,
                updatedAt = now
            )
        )
    }

    private fun calculateSpeedFrom(previous: Location?, current: Location): Float? {
        previous ?: return null
        if (previous.provider != current.provider) return null
        if (
            previous.hasAccuracy() &&
            current.hasAccuracy() &&
            maxOf(previous.accuracy, current.accuracy) > MAX_FALLBACK_ACCURACY_METERS
        ) {
            return null
        }
        val elapsedSeconds =
            (current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000f
        if (elapsedSeconds !in 0.5f..10f) return null
        return previous.distanceTo(current) / elapsedSeconds
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Divoom Arrow กำลังนำทาง")
            .setContentText("กำลังส่งแอนิเมชันไปยังกระเป๋า")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(LEGACY_GPS_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "แอนิเมชันระหว่างนำทาง",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "รักษาการเชื่อมต่อและปรับจังหวะแอนิเมชันระหว่างนำทาง"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopMotionTracking() {
        mainHandler.removeCallbacks(staleFixWatchdog)
        runCatching { locationManager.removeUpdates(this) }
        previousLocation = null
        smoothedSpeedKmh = null
        lastFixAt = 0L
        lastGpsFixAt = 0L
        stationarySince = 0L
        MotionStateBus.publish(MotionState(updatedAt = System.currentTimeMillis()))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopMotionTracking()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "navigation_connection"
        private const val LEGACY_GPS_CHANNEL_ID = "navigation_speed"
        private const val NOTIFICATION_ID = 2401
        private const val LOCATION_INTERVAL_MS = 1_000L
        private const val MIN_DISTANCE_METERS = 0.5f
        private const val FIX_STALE_AFTER_MS = 10_000L
        private const val WATCHDOG_INTERVAL_MS = 5_000L
        private const val PREFER_GPS_FOR_MS = 10_000L
        private const val METERS_PER_SECOND_TO_KMH = 3.6f
        private const val MAX_SPEED_KMH = 299
        private const val MAX_FALLBACK_ACCURACY_METERS = 50f
        private const val STATIONARY_NOISE_KMH = 1.5f
        private const val STOPPED_THRESHOLD_KMH = 4f
        private const val STOPPED_CONFIRMATION_MS = 6_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NavigationForegroundService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NavigationForegroundService::class.java))
        }
    }
}
