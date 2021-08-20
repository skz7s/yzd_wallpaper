package com.yzd.wallpaper.services

import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent

class YzdGestureListener(context: Context) : GestureDetector.SimpleOnGestureListener() {

    private val context = context


    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        if (e1?.y?.minus(e2?.y!!)!! > 10) {
            /* 上滑 */
            val intent = Intent(context, AutoService::class.java)
            intent.putExtra("operate", "ExchangeWallpaper")
            context.startService(intent)
            return super.onFling(e1, e2, velocityX, velocityY)
            return true;
        }
        else if (e2?.y?.minus(e1?.y!!)!! > 10) {
            /* 下滑 */
            val intent = Intent(context, AutoService::class.java)
            intent.putExtra("operate", "CollectWallpaper")
            context.startService(intent)
            return true;
        }
        return true
    }
}