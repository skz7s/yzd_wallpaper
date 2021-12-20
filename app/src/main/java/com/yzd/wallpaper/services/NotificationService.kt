package com.yzd.wallpaper.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import com.yzd.wallpaper.R


class NotificationService : Service() {
    private lateinit var notification: Notification.Builder
    private lateinit var receiver: MonitorBroadcast

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

    override fun onCreate() {
        super.onCreate()
        setBroadcast()
        notification = buildNotificationBuilder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifyMsg("点击切换当前壁纸")
        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(android.os.Process.myPid())
        }
        super.onDestroy()
    }


}