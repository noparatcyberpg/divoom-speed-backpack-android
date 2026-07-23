package com.divoomspeed.backpack.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.divoomspeed.backpack.logging.DebugLogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FusedSpeedTracker(
    context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : SpeedTracker {

    private val _speedState = MutableStateFlow(SpeedReading())
    override val speedState: StateFlow<SpeedReading> = _speedState.asStateFlow()

    private var useSmoothing: Boolean = true
    private var lastSmoothedSpeed: Float? = null
    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val hasSpeed = location.hasSpeed()
            val mps = if (hasSpeed) location.speed else 0f
            val accuracy = if (location.hasAccuracy()) location.accuracy else null

            val reading = SpeedReading.create(
                metersPerSecond = mps,
                hasSpeed = hasSpeed,
                accuracy = accuracy,
                timestamp = location.time,
                previousSmoothedKmh = lastSmoothedSpeed,
                useSmoothing = useSmoothing
            )

            if (reading.hasSpeed) {
                lastSmoothedSpeed = reading.speedKmh
            }

            _speedState.value = reading
            DebugLogger.d("GPS", "Speed update: ${reading.displaySpeedKmh} km/h (raw m/s: $mps, accuracy: $accuracy m)")
        }
    }

    override fun setSmoothingEnabled(enabled: Boolean) {
        useSmoothing = enabled
    }

    @SuppressLint("MissingPermission")
    override suspend fun start(): Result<Unit> {
        if (isTracking) return Result.success(Unit)

        return try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMaxUpdateDelayMillis(2000L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(true)
                .build()

            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isTracking = true
            DebugLogger.i("GPS", "FusedSpeedTracker started successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            DebugLogger.e("GPS", "Failed to start FusedSpeedTracker: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    override suspend fun stop() {
        if (!isTracking) return
        try {
            fusedClient.removeLocationUpdates(locationCallback)
            isTracking = false
            DebugLogger.i("GPS", "FusedSpeedTracker stopped")
        } catch (e: Exception) {
            DebugLogger.e("GPS", "Error stopping FusedSpeedTracker: ${e.localizedMessage}")
        }
    }
}
