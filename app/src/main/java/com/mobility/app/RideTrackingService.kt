package com.mobility.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import kotlin.math.max

class RideTrackingService : Service(), LocationListener {
    private lateinit var lm: LocationManager
    private var previous: Location? = null
    private var previousTime = 0L
    override fun onCreate() { super.onCreate(); lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager; if (Build.VERSION.SDK_INT >= 26) { val c = NotificationChannel(CHANNEL, "Active ride tracking", NotificationManager.IMPORTANCE_LOW); c.description = "Visible notification while recording an active ride"; (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c) } }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopTracking(); stopSelf(); return START_NOT_STICKY }
        if (intent?.action == START) { val id = intent.getStringExtra(EXTRA_ID).orEmpty().trim(); if (id.isBlank()) { stopSelf(); return START_NOT_STICKY }; TripStore.reset(this, id); previous = null; previousTime = 0L }
        else if (!TripMetrics.read(this).active) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, notification("GPS trip tracking is active"))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { stopTracking(); stopSelf(); return START_NOT_STICKY }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this, Looper.getMainLooper()) } catch (_: SecurityException) { stopTracking(); stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }
    override fun onLocationChanged(location: Location) {
        if (location.accuracy > 35f || !location.latitude.isFinite() || !location.longitude.isFinite()) return
        val now = location.time; val prev = previous; var delta = 0.0; var moving = 0L
        if (prev != null) {
            val dt = now - previousTime
            if (dt < 1000L || dt > 15000L) { previous = location; previousTime = now; return }
            val meters = prev.distanceTo(location).toDouble(); val seconds = dt / 1000.0
            if (meters > max(100.0, seconds * 55.0)) { previous = location; previousTime = now; return }
            delta = meters
            val speed = if (location.hasSpeed() && location.speed.isFinite()) location.speed.toDouble() else meters / seconds
            if (speed >= 0.8) moving = seconds.toLong().coerceAtLeast(0L)
        }
        val speedKmh = if (location.hasSpeed() && location.speed.isFinite()) (location.speed * 3.6).coerceAtLeast(0.0).toDouble() else 0.0
        TripStore.update(this, location, delta, moving, speedKmh); previous = location; previousTime = now
        val m = TripMetrics.read(this); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(String.format(java.util.Locale.US, "%.0f km/h · %.2f km", m.currentSpeedKmh, m.distanceMeters / 1000.0)))
    }
    private fun stopTracking() { try { lm.removeUpdates(this) } catch (_: Exception) {}; TripStore.finish(this); stopForeground(STOP_FOREGROUND_REMOVE) }
    override fun onDestroy() { try { lm.removeUpdates(this) } catch (_: Exception) {}; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    @Deprecated("Deprecated by Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    private fun notification(detail: String): Notification = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("MOBILITY · Active trip").setContentText(detail).setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    companion object {
        const val START = "com.mobility.app.START_TRIP"; const val STOP = "com.mobility.app.STOP_TRIP"; const val EXTRA_ID = "trip_id"
        private const val CHANNEL = "mobility_trip_tracking"; private const val NOTIFICATION_ID = 7214
        fun start(context: Context, id: String) { ContextCompat.startForegroundService(context, Intent(context, RideTrackingService::class.java).setAction(START).putExtra(EXTRA_ID, id)) }
        fun stop(context: Context) { context.startService(Intent(context, RideTrackingService::class.java).setAction(STOP)) }
    }
}
