package com.yzd.wallpaper.operate

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.yzd.wallpaper.utils.PreferenceUtils
import java.io.File

class CollectWallpaper(context: Context) {

    private val preferenceUtils = PreferenceUtils(context)
    private val wallpaperPath: String
    private val context = context

    companion object {
        private const val TAG = "CollectWallpaper"
    }

    init {
        val file = context.getExternalFilesDir("wallpaper")
        if (!file?.exists()!!) {
            file.mkdirs()
        }

        wallpaperPath = file.path
    }

    fun operate() {
        if (preferenceUtils.correctWallpaper != "") {
            val correctFile = File(preferenceUtils.correctWallpaper)
            Log.d(TAG, preferenceUtils.correctWallpaper.toString())
            val targetFile =
                File(wallpaperPath + File.separator + "我的收藏" + File.separator + correctFile.name)

            if(!targetFile.exists()){
                correctFile.copyTo(targetFile)
            }
        }
    }

}