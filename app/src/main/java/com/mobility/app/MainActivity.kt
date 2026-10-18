package com.mobility.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var root: LinearLayout; private lateinit var content: LinearLayout; private var web: WebView? = null; private var tab = "market"; private val ink = Color.rgb(10,12,12); private val panel = Color.rgb(22,25,24); private val lime = Color.rgb(198,255,72); private val muted = Color.rgb(161,169,162); private val permissionCode = 410; private var pendingId = ""
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor = ink; window.navigationBarColor = ink; shell(); market(); onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) { override fun handleOnBackPressed() { if (tab == "market" && web?.canGoBack() == true) web?.goBack() else if (tab != "market") market() else finish() } }) }
    private fun shell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(ink) }; content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(ink) }; root.addView(content, LinearLayout.LayoutParams(-1,0,1f))
        val nav = LinearLayout(this).apply { setBackgroundColor(Color.rgb(17,19,18)); setPadding(6,4,6,8) }
        nav.addView(navButton("Explore","market"), LinearLayout.LayoutParams(0,dp(50),1f)); nav.addView(navButton("My trip","trip"), LinearLayout.LayoutParams(0,dp(50),1f)); nav.addView(navButton("Trip history","history"), LinearLayout.LayoutParams(0,dp(50),1f)); root.addView(nav); setContentView(root)
    }
    private fun navButton(label: String, value: String) = Button(this).apply { text=label; isAllCaps=false; textSize=12f; setTextColor(if(tab==value) lime else Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { when(value) { "market" -> market(); "trip" -> trip(); else -> history() }; updateNav() } }
    private fun updateNav() { val nav = root.getChildAt(1) as LinearLayout; for(i in 0 until nav.childCount) (nav.getChildAt(i) as Button).setTextColor(if(listOf("market","trip","history")[i]==tab) lime else Color.WHITE) }
    private fun clear() { content.removeAllViews() }
    @SuppressLint("SetJavaScriptEnabled") private fun market() {
        tab="market"; clear(); val v=web ?: WebView(this).also { web=it }; v.setBackgroundColor(ink); v.settings.javaScriptEnabled=true; v.settings.domStorageEnabled=true; v.settings.loadsImagesAutomatically=true; v.settings.setGeolocationEnabled(true)
        v.webViewClient=object: WebViewClient() { override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean { val u=Uri.parse(url); if(u.scheme=="https" && u.host=="mobility-03.vercel.app") return false; startActivity(Intent(Intent.ACTION_VIEW,u)); return true } }
        v.webChromeClient=object: WebChromeClient() { override fun onGeolocationPermissionsShowPrompt(origin:String, callback:GeolocationPermissions.Callback) { val hasLocation = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; callback.invoke(origin, origin=="https://mobility-03.vercel.app" && hasLocation, false) } }
        content.addView(v,LinearLayout.LayoutParams(-1,-1)); if(v.url.isNullOrBlank()) v.loadUrl("https://mobility-03.vercel.app")
    }
    private fun trip() {
        tab="trip"; clear(); val scroll=ScrollView(this); val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(24),dp(22),dp(22)); setBackgroundColor(ink) }; scroll.addView(page); content.addView(scroll)
        page.addView(text("MOBILITY / RIDE TELEMETRY",11,lime)); page.addView(text("Your active trip",29,Color.WHITE,true).apply { setPadding(0,dp(10),0,dp(8)) }); page.addView(text("Tracking starts only after you confirm. A persistent Android notification stays visible during recording.",14,muted))
        val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(12)); setBackgroundColor(panel) }; val speed=text("0 km/h",34,lime,true); val distance=text("0.00 km distance",17,Color.WHITE,true); val avg=text("Average 0 km/h",14,muted); val top=text("Top speed 0 km/h",14,muted); val duration=text("Duration 00:00:00",14,muted); listOf(speed,distance,avg,top,duration).forEach { it.setPadding(0,dp(6),0,dp(6)); card.addView(it) }; page.addView(card,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(18) })
        val id=EditText(this).apply { hint="Authorized booking reference"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(muted); setPadding(dp(14),dp(12),dp(14),dp(12)); setBackgroundColor(panel) }; page.addView(id,LinearLayout.LayoutParams(-1,dp(54)).apply { topMargin=dp(18) })
        page.addView(Button(this).apply { text="Start GPS tracking"; isAllCaps=false; setTextColor(ink); setBackgroundColor(lime); setOnClickListener { val value=id.text.toString().trim(); if(value.isBlank()) id.error="Booking reference required" else if(TripMetrics.read(this@MainActivity).active) AlertDialog.Builder(this@MainActivity).setMessage("A trip is already active on this device.").setPositiveButton("OK",null).show() else AlertDialog.Builder(this@MainActivity).setTitle("Start trip tracking?").setMessage("Your phone location will be used to estimate distance, current speed, average speed and top speed for this rental. Tracking is visible and can be stopped at any time. This build stores trip data locally; it does not yet share live data with admins.").setNegativeButton("Cancel",null).setPositiveButton("Agree and start") { _,_-> requestAndStart(value) }.show() } },LinearLayout.LayoutParams(-1,dp(52)).apply { topMargin=dp(12) })
        page.addView(Button(this).apply { text="End trip"; isAllCaps=false; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(95,34,34)); setOnClickListener { if(TripMetrics.read(this@MainActivity).active) RideTrackingService.stop(this@MainActivity); updateMetrics(speed,distance,avg,top,duration) } },LinearLayout.LayoutParams(-1,dp(50)).apply { topMargin=dp(8) })
        page.addView(text("GPS estimates may fluctuate. Low-accuracy fixes and implausible jumps are ignored. These metrics are not a certified odometer.",12,muted).apply { setPadding(0,dp(18),0,0) })
        val handler=Handler(mainLooper); val tick=object:Runnable { override fun run() { if(tab!="trip") return; updateMetrics(speed,distance,avg,top,duration); handler.postDelayed(this,1000) } }; handler.post(tick)
    }
    private fun updateMetrics(speed:TextView,distance:TextView,avg:TextView,top:TextView,duration:TextView) { val m=TripMetrics.read(this); speed.text=String.format(Locale.getDefault(),"%.0f km/h",m.currentSpeedKmh); distance.text=String.format(Locale.getDefault(),"%.2f km distance",m.distanceMeters/1000.0); avg.text=String.format(Locale.getDefault(),"Average %.1f km/h (moving time)",m.averageSpeedKmh); top.text=String.format(Locale.getDefault(),"Top speed %.1f km/h",m.maxSpeedKmh); duration.text="Duration "+format(m.elapsedSeconds)+if(m.active) " · TRACKING" else " · ENDED" }
    private fun history() { tab="history"; clear(); val p=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(28),dp(22),dp(22)); setBackgroundColor(ink) }; content.addView(p); val m=TripMetrics.read(this); p.addView(text("MOBILITY / TRIP RECORD",11,lime)); p.addView(text("Trip summary",28,Color.WHITE,true).apply { setPadding(0,dp(12),0,dp(12)) }); p.addView(text(if(m.tripId.isBlank()) "No trip recorded on this device yet." else "Booking reference: "+m.tripId,15,Color.WHITE)); p.addView(text(String.format(Locale.getDefault(),"Distance  %.2f km",m.distanceMeters/1000.0),16,muted).apply { setPadding(0,dp(12),0,dp(4)) }); p.addView(text(String.format(Locale.getDefault(),"Average speed  %.1f km/h",m.averageSpeedKmh),16,muted).apply { setPadding(0,dp(4),0,dp(4)) }); p.addView(text(String.format(Locale.getDefault(),"Top speed  %.1f km/h",m.maxSpeedKmh),16,muted).apply { setPadding(0,dp(4),0,dp(4)) }); p.addView(text("This is local device history only, not yet synced to the website or visible to admins on another device.",13,muted).apply { setPadding(0,dp(20),0,0) }) }
    private fun requestAndStart(id:String) { val required= mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION); if(Build.VERSION.SDK_INT>=33) required.add(Manifest.permission.POST_NOTIFICATIONS); val missing=required.filter { ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }; if(missing.isEmpty()) startTrip(id) else { pendingId=id; ActivityCompat.requestPermissions(this,missing.toTypedArray(),permissionCode) } }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,results:IntArray) { super.onRequestPermissionsResult(requestCode,permissions,results); if(requestCode==permissionCode) { if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED && pendingId.isNotBlank()) startTrip(pendingId) else AlertDialog.Builder(this).setMessage("Precise location is required. Tracking has not started.").setPositiveButton("OK",null).show(); pendingId="" } }
    private fun startTrip(id:String) { RideTrackingService.start(this,id); trip() }
    private fun text(value:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply { text=value; textSize=size.toFloat(); setTextColor(color); gravity=Gravity.START; if(bold) setTypeface(typeface,android.graphics.Typeface.BOLD) }
    private fun format(s:Long)=String.format(Locale.getDefault(),"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
