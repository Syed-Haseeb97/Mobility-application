package com.mobility.app

import android.content.Context
import android.location.Location
import kotlin.math.max

data class TripMetrics(val tripId: String, val startedAt: Long, val distanceMeters: Double, val movingSeconds: Long, val maxSpeedKmh: Double, val currentSpeedKmh: Double, val active: Boolean) {
    val averageSpeedKmh: Double get() = if (movingSeconds > 0) (distanceMeters / 1000.0) / (movingSeconds / 3600.0) else 0.0
    val elapsedSeconds: Long get() = if (startedAt > 0) ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0) else 0
    companion object {
        private const val PREFS = "mobility_trip"
        fun read(context: Context): TripMetrics {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return TripMetrics(p.getString("trip_id", "") ?: "", p.getLong("started_at", 0L), p.getFloat("distance_m", 0f).toDouble(), p.getLong("moving_s", 0L), p.getFloat("max_kmh", 0f).toDouble(), p.getFloat("current_kmh", 0f).toDouble(), p.getBoolean("active", false))
        }
    }
}

internal object TripStore {
    private const val PREFS = "mobility_trip"
    fun reset(context: Context, id: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().putString("trip_id", id.trim()).putLong("started_at", System.currentTimeMillis()).putBoolean("active", true).apply() }
    fun update(context: Context, location: Location, delta: Double, moving: Long, speed: Double) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putFloat("distance_m", (p.getFloat("distance_m", 0f) + max(0.0, delta).toFloat())).putLong("moving_s", p.getLong("moving_s", 0L) + moving).putFloat("current_kmh", speed.toFloat()).putFloat("max_kmh", max(p.getFloat("max_kmh", 0f).toDouble(), speed).toFloat()).putFloat("lat", location.latitude.toFloat()).putFloat("lon", location.longitude.toFloat()).apply()
    }
    fun finish(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("active", false).putFloat("current_kmh", 0f).apply() }
}
