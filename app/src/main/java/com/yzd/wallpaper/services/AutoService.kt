package com.yzd.wallpaper.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.yzd.wallpaper.operate.CollectWallpaper
import com.zhy.http.okhttp.OkHttpUtils
import com.zhy.http.okhttp.cookie.CookieJarImpl
import com.zhy.http.okhttp.log.LoggerInterceptor
import okhttp3.OkHttpClient
import com.yzd.wallpaper.operate.ExchangeWallpaper
import com.yzd.wallpaper.operate.UpdateWallpaper
import com.yzd.wallpaper.utils.PersistentCookieStore
import java.lang.Exception
import java.util.concurrent.TimeUnit


class AutoService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        this.getExternalFilesDir("temp")?.delete()
        val cookieJar = CookieJarImpl(
            PersistentCookieStore(
                this
            )
        )

        val okHttpClient: OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(LoggerInterceptor("HttpClient"))
                .cookieJar(cookieJar)
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS) //其他配置
                .build()

        OkHttpUtils.initClient(okHttpClient)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val operate = intent?.getStringExtra("operate")
        val isAuto = intent?.getBooleanExtra("isAuto", false)
        Log.v(TAG, operate.toString() + " " + isAuto.toString())
        when (operate) {
            "ExchangeWallpaper" -> {
                val exchangeWallpaper =
                    ExchangeWallpaper(this)
                Thread(Runnable {
                    try {
                        exchangeWallpaper.operate(isAuto != null && isAuto)
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()

            }
            "UpdateWallpaper" -> {
                val updateWallpaper =
                    UpdateWallpaper(this)
                Thread(Runnable {
                    try {
                        updateWallpaper.operate(isAuto != null && isAuto)
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()
            }
            "CollectWallpaper" -> {
                val collectWallpaper =
                    CollectWallpaper(this)
                try {
                    collectWallpaper.operate()
                    Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AutoService"
    }


}