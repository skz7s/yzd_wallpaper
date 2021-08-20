package com.yzd.wallpaper.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


class PreferenceUtils(context: Context?) {

    val defaultPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val isServiceOpen = defaultPrefs.getBoolean("is_service_open", false)
    val isLocalPath = defaultPrefs.getBoolean("is_local_path", false)
    val wallpaperLib: MutableSet<String>? =
        defaultPrefs.getStringSet("wallpaper_lib", mutableSetOf<String>())

    val customPath = defaultPrefs.getString("custom_path", "")
    val isAutoUpdate = defaultPrefs.getBoolean("is_auto_update", false)
    val strategy: MutableSet<String>? = defaultPrefs.getStringSet(
        "strategy",
        mutableSetOf<String>()
    )

    val totalNum = defaultPrefs.getString("total_num", "30")
    val isHideTask = defaultPrefs.getBoolean("is_hide_task", false)
    val isGesture = defaultPrefs.getBoolean("is_gesture", false)

    val correctWallpaper = defaultPrefs.getString("correct_wallpaper", "")
    val correctVersion = defaultPrefs.getString("correct_version", "")

    companion object : SingletonHolder<PreferenceUtils, Context>(::PreferenceUtils)

    fun setValue(key:String, value:String){
        val writer = defaultPrefs.edit()
        writer.putString(key, value)
        writer.apply()
    }

    fun setValue(key:String, value:Boolean){
        val writer = defaultPrefs.edit()
        writer.putBoolean(key, value)
        writer.apply()
    }

    fun setValue(key:String, value:MutableSet<String>){
        val writer = defaultPrefs.edit()
        writer.putStringSet(key, value)
        writer.apply()
    }
}