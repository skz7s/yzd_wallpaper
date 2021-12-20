package com.yzd.wallpaper.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


class PreferenceUtils(context: Context?) {

    private val defaultPrefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    companion object : SingletonHolder<PreferenceUtils, Context>(::PreferenceUtils)

    fun setValue(key: String, value: String) {
        val writer = defaultPrefs.edit()
        writer.putString(key, value)
        writer.apply()
    }

    fun setValue(key: String, value: Boolean) {
        val writer = defaultPrefs.edit()
        writer.putBoolean(key, value)
        writer.apply()
    }

    fun setValue(key: String, value: MutableSet<String>) {
        val writer = defaultPrefs.edit()
        writer.putStringSet(key, value)
        writer.apply()
    }
}