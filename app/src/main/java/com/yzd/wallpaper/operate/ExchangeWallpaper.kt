package com.yzd.wallpaper.operate

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ExchangeWallpaper(context: Context) {

    private val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val isServerOpen = prefs.getBoolean("is_service_open", false)
    private val wallpaperPath = context.getExternalFilesDir("wallpaper") as File

    companion object {
        private const val TAG = "ExchangeWallpaper"
    }

    private fun isOn(): Boolean {
        return isServerOpen
    }


    private fun isImagePath(path: String): Boolean {
        return path.endsWith("jpg") || path.endsWith("png") || path.endsWith("jpeg")
    }

    private fun getWallLibPaths(): ArrayList<String> {
        val wallArray = ArrayList<String>()
        if (wallpaperPath.exists()) {
            for (item in wallpaperPath.listFiles()) {
                if (item.isDirectory) {
                    wallArray.add(item.name)
                }
            }
        }
        return wallArray
    }

    private fun getWallpaperList(): ArrayList<String> {
        val wallpaperList = ArrayList<String>()

        /*非自定义目录，获取配置，然后在获取图片*/
        for (wallLib in getWallLibPaths()) {
            if (wallLib != null) {
                for (item in File(wallpaperPath.path + File.separator + wallLib).listFiles()) {
                    if (isImagePath(item.path)) {
                        wallpaperList.add(item.path)
                    }
                }
            }
        }

        return wallpaperList
    }

    fun operate() {
        val wallpaperList = getWallpaperList()
        if (wallpaperList.isNotEmpty()) {
            val index = (Math.random() * wallpaperList.size).toInt()
            val wallpaper = wallpaperList[index]
            Log.d(TAG, "随机壁纸：$wallpaper")
            val wallpaperStream: InputStream = FileInputStream(wallpaper)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val bmp = BitmapFactory.decodeFile(wallpaper)
                this.wallpaperManager.setStream(
                    wallpaperStream,
                    Rect(0, 0, bmp.width, bmp.height),
                    false,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                )
            } else {
                this.wallpaperManager.setStream(wallpaperStream)
            }

        } else {
            Log.d(TAG, "没有找到壁纸")
        }

    }

    fun autoOperate() {
        if (isOn()) {
            operate()
        }
    }
}