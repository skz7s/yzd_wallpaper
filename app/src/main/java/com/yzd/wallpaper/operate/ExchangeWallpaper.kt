package com.yzd.wallpaper.operate

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.util.Log
import com.yzd.wallpaper.utils.PreferenceUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ExchangeWallpaper(context: Context) {

    private val preferenceUtils = PreferenceUtils(context)
    private val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)
    private val wallpaperPath = context.getExternalFilesDir("wallpaper") as File

    companion object {
        private const val TAG = "ExchangeWallpaper"
    }

    private fun isOn(): Boolean {
        return preferenceUtils.isServiceOpen
    }


    private fun isImagePath(path: String): Boolean {
        return path.endsWith("jpg") || path.endsWith("png") || path.endsWith("jpeg")
    }

    private fun getWallLibPaths(): ArrayList<String> {
        val wallArray = ArrayList<String>()
        val wallLibSet = preferenceUtils.wallpaperLib
        if (wallLibSet != null) {
            if (wallLibSet.isEmpty()) {
                /*空表示则为所有配置*/
                if (wallpaperPath.exists()) {
                    for (item in wallpaperPath.listFiles()) {
                        if (item.isDirectory) {
                            wallArray.add(item.name)
                        }
                    }
                }
            } else {
                for (item in wallLibSet) {
                    wallArray.add(item)
                }
            }
        }
        return wallArray
    }

    private fun getWallpaperList(): ArrayList<String> {
        val wallpaperList = ArrayList<String>()

        if (!preferenceUtils.isLocalPath && preferenceUtils.customPath != "") {
            /*自定义目录，直接获取下面的所有图片*/
            val fileList = File(preferenceUtils.customPath.toString()).listFiles()
            if (fileList != null) {
                for (item in fileList) {
                    if (isImagePath(item.path)) {
                        wallpaperList.add(item.path)
                    }
                }
            }
        } else {
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
        }

        return wallpaperList
    }

    fun operate(isAuto:Boolean) {
        if (! isAuto || isOn()) {
            val wallpaperList = getWallpaperList()
            if (wallpaperList.isNotEmpty()) {
                var index = (Math.random() * wallpaperList.size).toInt()
                if (preferenceUtils.correctWallpaper == wallpaperList[index]){
                    index = wallpaperList.size - index - 1
                }
                val wallpaper = wallpaperList[index]
                Log.d(TAG, "随机壁纸：$wallpaper")
                val writer = preferenceUtils.defaultPrefs.edit()
                writer.putString("correct_wallpaper", wallpaper)
                writer.apply()
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
    }
}