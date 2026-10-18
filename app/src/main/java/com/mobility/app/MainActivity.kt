package com.mobility.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var navWrap: LinearLayout
    private var web: WebView? = null
    private var tab = "market"
    private var topInset = 0
    private var bottomInset = 0
    private val ink = Color.rgb(10, 12, 12)
    private val panel = Color.rgb(22, 25, 24)
    private val raised = Color.rgb(29, 33, 31)
    private val lime = Color.rgb(198, 255, 72)
    private val muted = Color.rgb(161, 169, 162)
    private val softWhite = Color.rgb(232, 236, 231)
    private val permissionCode = 410
    private var pendingId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        shell()
        market()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tab == "market" && web?.canGoBack() == true) web?.goBack()
                else if (tab != "market") market()
                else finish()
            }
        })
    }

    private fun shell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
            clipChildren = false
            clipToPadding = false
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
            clipChildren = false
            clipToPadding = false
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = rounded(raised, 28)
            elevation = dp(12).toFloat()
            clipToOutline = true
        }
        nav.addView(navButton("⌂", "Explore", "market"), LinearLayout.LayoutParams(0, dp(52), 1f))
        nav.addView(navButton("◉", "My trip", "trip"), LinearLayout.LayoutParams(0, dp(52), 1f))
        nav.addView(navButton("↻", "History", "history"), LinearLayout.LayoutParams(0, dp(52), 1f))
        navWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(7), dp(18), dp(10))
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            addView(nav, LinearLayout.LayoutParams(-1, dp(64)))
        }
        root.addView(navWrap, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            topInset = bars.top
            bottomInset = bars.bottom
            navWrap.setPadding(dp(18), dp(7), dp(18), bottomInset + dp(8))
            if (tab != "market") {
                val page = ((content.getChildAt(0) as? ScrollView)?.getChildAt(0) as? LinearLayout)
                page?.setPadding(dp(22), topInset + dp(22), dp(22), dp(28))
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun navButton(symbol: String, label: String, value: String) = Button(this).apply {
        text = "$symbol  $label"
        isAllCaps = false
        textSize = 12f
        letterSpacing = 0.01f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (tab == value) ink else softWhite)
        background = rounded(if (tab == value) lime else Color.TRANSPARENT, 22)
        elevation = if (tab == value) dp(2).toFloat() else 0f
        setPadding(dp(3), 0, dp(3), 0)
        minHeight = 0
        minimumHeight = 0
        stateListAnimator = null
        setOnClickListener {
            when (value) {
                "market" -> market()
                "trip" -> trip()
                else -> history()
            }
            updateNav()
        }
    }

    private fun updateNav() {
        if (!::navWrap.isInitialized || navWrap.childCount == 0) return
        val nav = navWrap.getChildAt(0) as? LinearLayout ?: return
        val values = listOf("market", "trip", "history")
        for (i in 0 until nav.childCount) {
            val button = nav.getChildAt(i) as Button
            val selected = values[i] == tab
            button.setTextColor(if (selected) ink else softWhite)
            button.background = rounded(if (selected) lime else Color.TRANSPARENT, 22)
            button.elevation = if (selected) dp(2).toFloat() else 0f
        }
    }

    private fun clear() = content.removeAllViews()

    @SuppressLint("SetJavaScriptEnabled")
    private fun market() {
        tab = "market"
        clear()
        val view = web ?: WebView(this).also { web = it }
        view.setBackgroundColor(ink)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.loadsImagesAutomatically = true
        view.settings.setGeolocationEnabled(true)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == "mobility-iiitt-mu.vercel.app") return false
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                return true
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                val allowed = origin == "https://mobility-iiitt-mu.vercel.app" &&
                    (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                     ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                callback.invoke(origin, allowed, false)
            }
        }
        content.addView(view, LinearLayout.LayoutParams(-1, -1))
        if (view.url.isNullOrBlank()) view.loadUrl("https://mobility-iiitt-mu.vercel.app")
        updateNav()
    }

    private fun trip() {
        tab = "trip"
        clear()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setBackgroundColor(ink) }
        val page = pageColumn()
        scroll.addView(page, FrameLayout.LayoutParams(-1, -2))
        content.addView(scroll, LinearLayout.LayoutParams(-1, -1))

        page.addView(eyebrow("MOBILITY  /  RIDE TELEMETRY"))
        page.addView(text("Your trip,\nin control.", 32, Color.WHITE, true).apply { setPadding(0, dp(10), 0, dp(8)); setLineSpacing(dp(1).toFloat(), 1f) })
        page.addView(text("Live ride metrics from your phone. Tracking only begins after you approve it.", 14, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) })

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(24, 39, 24), 14)
        }
        status.addView(View(this).apply { background = rounded(lime, 20) }, LinearLayout.LayoutParams(dp(7), dp(7)))
        status.addView(text("  GPS tracking is off", 12, lime, true))
        page.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val speed = metricTile(row1, "CURRENT SPEED", "0", "km/h", true, true)
        val distance = metricTile(row1, "DISTANCE", "0.00", "km", false, false)
        val avg = metricTile(row2, "AVERAGE SPEED", "0.0", "km/h", false, false)
        val top = metricTile(row2, "TOP SPEED", "0.0", "km/h", false, false)
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(row1); addView(row2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }) }
        page.addView(metrics, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        val duration = text("Duration  00:00:00", 13, softWhite, true)
        val durationCard = surface().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text("SESSION TIME", 11, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(duration)
        }
        page.addView(durationCard, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(12) })

        page.addView(eyebrow("TRIP DETAILS").apply { setPadding(0, dp(26), 0, dp(10)) })
        val booking = EditText(this).apply {
            hint = "Enter your booking reference"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(raised, 14)
        }
        page.addView(booking, LinearLayout.LayoutParams(-1, dp(54)))
        page.addView(actionButton("Start GPS tracking", true) {
            val id = booking.text.toString().trim()
            if (id.isBlank()) booking.error = "Booking reference required"
            else if (TripMetrics.read(this@MainActivity).active) {
                AlertDialog.Builder(this@MainActivity).setMessage("A trip is already active on this device.").setPositiveButton("OK", null).show()
            } else {
                AlertDialog.Builder(this@MainActivity).setTitle("Start trip tracking?")
                    .setMessage("Your phone location will estimate distance and speed for this rental. A persistent notification stays visible, and you can stop tracking at any time. This build stores trip data on this device only.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Agree and start") { _, _ -> requestAndStart(id) }.show()
            }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        page.addView(actionButton("End trip", false) {
            if (TripMetrics.read(this@MainActivity).active) RideTrackingService.stop(this@MainActivity)
            updateMetrics(speed, distance, avg, top, duration)
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })
        page.addView(text("GPS estimates can fluctuate. Low-accuracy fixes and implausible jumps are ignored. These values are not a certified odometer.", 12, muted).apply {
            setLineSpacing(dp(3).toFloat(), 1f); setPadding(0, dp(16), 0, dp(8))
        })

        val handler = Handler(mainLooper)
        val tick = object : Runnable {
            override fun run() {
                if (tab != "trip") return
                updateMetrics(speed, distance, avg, top, duration)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tick)
        updateNav()
    }

    private fun metricTile(row: LinearLayout, label: String, initial: String, unit: String, featured: Boolean, first: Boolean): TextView {
        val tile = surface().apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(12), dp(12))
            if (featured) background = rounded(Color.rgb(29, 42, 23), 18)
            addView(text(label, 9, if (featured) lime else muted, true).apply { letterSpacing = 0.06f })
        }
        val value = text(initial, 28, if (featured) lime else Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(1)); maxLines = 1 }
        tile.addView(value)
        tile.addView(text(unit, 11, muted))
        row.addView(tile, LinearLayout.LayoutParams(0, dp(116), 1f).apply {
            if (first) marginEnd = dp(6) else marginStart = dp(6)
        })
        return value
    }

    private fun updateMetrics(speed: TextView, distance: TextView, avg: TextView, top: TextView, duration: TextView) {
        val m = TripMetrics.read(this)
        speed.text = String.format(Locale.getDefault(), "%.0f", m.currentSpeedKmh)
        distance.text = String.format(Locale.getDefault(), "%.2f", m.distanceMeters / 1000.0)
        avg.text = String.format(Locale.getDefault(), "%.1f", m.averageSpeedKmh)
        top.text = String.format(Locale.getDefault(), "%.1f", m.maxSpeedKmh)
        duration.text = "Duration  " + format(m.elapsedSeconds) + if (m.active) "  ·  TRACKING" else "  ·  ENDED"
    }

    private fun history() {
        tab = "history"
        clear()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setBackgroundColor(ink) }
        val page = pageColumn()
        scroll.addView(page, FrameLayout.LayoutParams(-1, -2))
        content.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        val m = TripMetrics.read(this)

        page.addView(eyebrow("MOBILITY  /  YOUR ACTIVITY"))
        page.addView(text("Trip history", 32, Color.WHITE, true).apply { setPadding(0, dp(10), 0, dp(8)) })
        page.addView(text("A private record of the latest trip saved on this device.", 14, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) })

        val summary = surface().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        summary.addView(text(if (m.tripId.isBlank()) "NO TRIP RECORDED" else "LATEST TRIP", 10, lime, true).apply { letterSpacing = 0.08f })
        summary.addView(text(if (m.tripId.isBlank()) "Your next journey\nstarts here." else "Trip summary", 23, Color.WHITE, true).apply { setPadding(0, dp(10), 0, dp(8)) })
        summary.addView(text(if (m.tripId.isBlank()) "When you finish a tracked ride, its latest summary will appear here." else "Booking reference  ·  " + m.tripId, 13, muted).apply { setLineSpacing(dp(3).toFloat(), 1f) })
        page.addView(summary, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })

        val stats = surface().apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), dp(8)) }
        addHistoryRow(stats, "Distance travelled", String.format(Locale.getDefault(), "%.2f km", m.distanceMeters / 1000.0))
        addHistoryRow(stats, "Average speed", String.format(Locale.getDefault(), "%.1f km/h", m.averageSpeedKmh))
        addHistoryRow(stats, "Top speed", String.format(Locale.getDefault(), "%.1f km/h", m.maxSpeedKmh))
        addHistoryRow(stats, "Trip status", if (m.active) "In progress" else if (m.tripId.isBlank()) "No trip yet" else "Ended")
        page.addView(stats, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        val note = surface().apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(text("i", 16, lime, true).apply { gravity = Gravity.CENTER; background = rounded(Color.rgb(39, 52, 30), 20) },
                LinearLayout.LayoutParams(dp(30), dp(30)))
            addView(text("Saved locally on this phone. Trip records are not yet synced to the website or visible to admins on another device.", 12, muted).apply {
                setLineSpacing(dp(3).toFloat(), 1f)
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        }
        page.addView(note, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        page.addView(actionButton("Check for app updates", false) { checkForUpdates() }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(18) })
        page.addView(text("Get the latest Android build directly from MOBILITY. Downloads are verified by Android before installation.", 12, muted).apply {
            setLineSpacing(dp(3).toFloat(), 1f); setPadding(0, dp(8), 0, dp(4))
        })
        page.addView(actionButton("Back to Explore", true) { market() }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        updateNav()
    }

    private fun addHistoryRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(13), 0, dp(13))
            addView(text(label, 13, muted), LinearLayout.LayoutParams(0, -2, 1f))
            addView(text(value, 13, Color.WHITE, true))
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        if (label != "Trip status") parent.addView(View(this).apply { setBackgroundColor(Color.rgb(46, 51, 47)) }, LinearLayout.LayoutParams(-1, dp(1)))
    }


    private fun checkForUpdates() {
        AlertDialog.Builder(this)
            .setTitle("Checking for updates")
            .setMessage("Looking for the latest MOBILITY Android build…")
            .setCancelable(false)
            .show()
            .also { dialog ->
                Thread {
                    try {
                        val connection = (URL("https://api.github.com/repos/Syed-Haseeb97/Mobility-application/releases/tags/android-latest").openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 10000
                            readTimeout = 10000
                            setRequestProperty("Accept", "application/vnd.github+json")
                            setRequestProperty("User-Agent", "MOBILITY-Android-Updater")
                        }
                        val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                        connection.disconnect()
                        val body = release.optString("body")
                        val latestCode = Regex("versionCode\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
                            .find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        val assetUrl = release.optJSONArray("assets")?.let { assets ->
                            (0 until assets.length()).map { assets.getJSONObject(it) }
                                .firstOrNull { it.optString("name") == "MOBILITY-latest.apk" }
                                ?.optString("browser_download_url")
                        }.orEmpty()
                        val packageInfo = packageManager.getPackageInfo(packageName, 0)
                        val currentCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
                        runOnUiThread {
                            dialog.dismiss()
                            when {
                                latestCode <= 0L || assetUrl.isBlank() ->
                                    AlertDialog.Builder(this).setTitle("Updates not configured")
                                        .setMessage("A downloadable signed build is not published yet. Try again after the update channel is configured.")
                                        .setPositiveButton("OK", null).show()
                                latestCode > currentCode ->
                                    AlertDialog.Builder(this).setTitle("Update available")
                                        .setMessage("Installed version: ${packageInfo.versionName ?: currentCode}\nLatest version: $latestCode\n\nDownload the update now? Android will ask you to confirm installation.")
                                        .setNegativeButton("Later", null)
                                        .setPositiveButton("Download") { _, _ -> downloadUpdate(assetUrl) }.show()
                                else ->
                                    AlertDialog.Builder(this).setTitle("You're up to date")
                                        .setMessage("MOBILITY is already on the latest published Android build.")
                                        .setPositiveButton("Great", null).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            dialog.dismiss()
                            AlertDialog.Builder(this).setTitle("Couldn't check for updates")
                                .setMessage("Check your internet connection and try again. If no signed release has been published yet, the update channel may not be ready.")
                                .setPositiveButton("OK", null).show()
                        }
                    }
                }.start()
            }
    }

    private fun downloadUpdate(url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(this)
                .setTitle("Allow MOBILITY to install updates")
                .setMessage("Android requires permission to install an APK downloaded by this app. Enable “Allow from this source”, then tap Check for app updates again.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }.show()
            return
        }
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("MOBILITY Android update")
                .setDescription("Downloading the latest signed MOBILITY build")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MOBILITY-latest.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            getSharedPreferences("mobility_updates", MODE_PRIVATE).edit().putLong("download_id", id).apply()
            AlertDialog.Builder(this)
                .setTitle("Download started")
                .setMessage("Android will notify you when the APK is ready, then open its installer. Keep MOBILITY installed until the update is ready.")
                .setPositiveButton("OK", null).show()
        } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Download failed")
                .setMessage("Couldn't start the APK download. Please try again.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun requestAndStart(id: String) {
        val required = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = required.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startTrip(id)
        else {
            pendingId = id
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), permissionCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == permissionCode) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && pendingId.isNotBlank()) startTrip(pendingId)
            else AlertDialog.Builder(this).setMessage("Precise location is required. Tracking has not started.").setPositiveButton("OK", null).show()
            pendingId = ""
        }
    }

    private fun startTrip(id: String) {
        RideTrackingService.start(this, id)
        trip()
    }

    private fun pageColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), topInset + dp(22), dp(22), dp(28))
        setBackgroundColor(ink)
    }

    private fun eyebrow(value: String) = text(value, 10, lime, true).apply { letterSpacing = 0.12f }

    private fun surface() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(panel, 18)
    }

    private fun actionButton(label: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (primary) ink else softWhite)
        background = rounded(if (primary) lime else Color.rgb(53, 35, 35), 14)
        stateListAnimator = null
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        gravity = Gravity.START
        includeFontPadding = false
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun format(s: Long) = String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
