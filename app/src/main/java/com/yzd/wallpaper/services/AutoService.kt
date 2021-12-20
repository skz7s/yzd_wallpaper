package com.yzd.wallpaper.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
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
        applicationContext.getExternalFilesDir("temp")?.delete()
        val cookieJar = CookieJarImpl(
            PersistentCookieStore(
                applicationContext
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
        Log.v(TAG, operate.toString())
        Log.v(TAG, isAuto.toString())
        if (operate == "ExchangeWallpaper") {
            val exchangeWallpaper =
                ExchangeWallpaper(applicationContext)
            if (isAuto == null || !isAuto) {
                Thread(Runnable {
                    try {
                        exchangeWallpaper.operate()
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()
            } else {
                Thread(Runnable {
                    try {
                        exchangeWallpaper.autoOperate()
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()
            }
        } else if (operate == "UpdateWallpaper") {
            val updateWallpaper =
                UpdateWallpaper(applicationContext)
            if (isAuto == null || !isAuto) {
                Thread(Runnable {
                    try {
                        updateWallpaper.operate()
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()
            } else {
                Thread(Runnable {
                    try {
                        updateWallpaper.autoOperate()
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                }).start()
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