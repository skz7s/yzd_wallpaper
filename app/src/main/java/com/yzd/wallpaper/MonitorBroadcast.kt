package com.yzd.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.yzd.wallpaper.services.AutoService
import com.yzd.wallpaper.services.NotificationService

class MonitorBroadcast : BroadcastReceiver() {

    companion object {
        private const val TAG = "MonitorBroadcast"
    }

    override fun onReceive(ctx: Context, intent: Intent?) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        if (prefs.getBoolean("is_service_open", false)) {
            val action=intent?.action;
            if (Intent.ACTION_SCREEN_OFF == action){
                Log.d(TAG, "锁屏，切换壁纸")
                val intent = Intent(ctx, AutoService::class.java)
                intent.putExtra("operate", "ExchangeWallpaper")
                ctx.startService(intent)
            }else if (Intent.ACTION_BOOT_COMPLETED == action){
                Log.d(TAG, "自启动")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(Intent(ctx, NotificationService::class.java))
                }else{
                    ctx.startService(Intent(ctx, NotificationService::class.java))
                }
            }
        }
    }
}