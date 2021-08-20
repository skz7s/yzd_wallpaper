package com.yzd.wallpaper.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.yzd.wallpaper.MonitorBroadcast
import com.yzd.wallpaper.R
import com.yzd.wallpaper.utils.PreferenceUtils
import java.util.*


class NotificationService : Service() {
    private lateinit var notification: Notification.Builder
    private lateinit var receiver: MonitorBroadcast
    private lateinit var view: View

    companion object {
        private const val TAG = "NotificationService"
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    @SuppressLint("ResourceAsColor")
    private fun buildNotificationBuilder(): Notification.Builder {
        val intent = Intent(this, AutoService::class.java)
        intent.putExtra("operate", "ExchangeWallpaper")
        val pendingIntent =
            PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification: Notification.Builder //创建服务对象
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel =
                NotificationChannel(
                    "com.yzd.wallpaper",
                    "wallpaper",
                    NotificationManager.IMPORTANCE_MIN
                )
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.setSound(null, null)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(channel)

            notification = Notification.Builder(this, channel.id)
        } else {
            notification = Notification.Builder(this).setSound(null)
        }

        return notification.setSmallIcon(R.mipmap.ico)
            .setColor(R.color.colorPrimary)
            .setContentIntent(pendingIntent)
            .setContentTitle("全自动壁纸")
    }

    private fun notifyMsg(msg: String) {
        startForeground(
            android.os.Process.myPid(),
            notification
                .setContentText(msg)
                .build()
        )

    }

    private fun setBroadcast() {
        receiver = MonitorBroadcast()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, filter)
    }

    private fun openWindow() {
        val windowManager =
            this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams()

        val metric = resources.displayMetrics
        val width = metric.widthPixels
        val height = metric.heightPixels

        layoutParams.height = height / 10 * 9
        layoutParams.width = width / 20
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        layoutParams.format = PixelFormat.TRANSLUCENT //透明

        layoutParams.gravity = Gravity.CENTER or Gravity.RIGHT //右上角显示
        val gestureDetector = GestureDetector(this,
            YzdGestureListener(this)
        )
        val gestureListener: View.OnTouchListener =
            View.OnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        view.setOnTouchListener(gestureListener)

        windowManager.addView(view, layoutParams)
    }


    private fun setGesture() {
        if (PreferenceUtils(this).isGesture) {
            openWindow()
        }
    }

    override fun onCreate() {
        super.onCreate()
        setGesture()
        setBroadcast()
        notification = buildNotificationBuilder()
    }

    private fun setUpdateAlarm(
        alarmManager: AlarmManager,
        isAutoUpdate: Boolean,
        updateFreq: Long
    ) {

        val intentUpdate = Intent(this, AutoService::class.java)
        intentUpdate.putExtra("operate", "UpdateWallpaper")
        intentUpdate.putExtra("isAuto", true)
        val pendingIntentUpdate =
            PendingIntent.getService(
                this,
                1,
                intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        if (isAutoUpdate) {
            Log.d(TAG, "注册更新任务")
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().timeInMillis,
                updateFreq,
                pendingIntentUpdate
            )
        } else {
            Log.d(TAG, "取消更新任务")
            alarmManager.cancel(pendingIntentUpdate)
        }
    }

    private fun setAlarm() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val preferenceUtils = PreferenceUtils(this)

        val isAutoUpdate = preferenceUtils.isServiceOpen && preferenceUtils.isAutoUpdate

        val updateFreq = try {
            Integer.parseInt(prefs.getString("update_freq", "1")) * 3600 * 1000.toLong()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            36000000L
        }

        val alarmManager =
            this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        setUpdateAlarm(alarmManager, isAutoUpdate, updateFreq)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifyMsg("点击切换当前壁纸")
        if(intent != null && ! intent.getBooleanExtra("is_update", false)){
            setAlarm()
        }
        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        setAlarm()
        unregisterReceiver(receiver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(android.os.Process.myPid())
        }
        super.onDestroy()
    }


}