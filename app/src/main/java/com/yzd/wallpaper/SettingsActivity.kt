package com.yzd.wallpaper

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.alibaba.fastjson.JSONObject
import com.yzd.wallpaper.utils.PreferenceUtils


class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private fun getVersionName(): String? {
        val packInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        return packInfo.versionName
    }

    private fun updateData() {
        val preferenceUtils = PreferenceUtils(this)
        val oldVersion = preferenceUtils.correctVersion
        val correctionVersion = getVersionName()
        if (oldVersion != null && correctionVersion != null && oldVersion != "" && oldVersion < correctionVersion) {
            var strategy = mutableSetOf<String>()
            for(item in preferenceUtils.strategy!!){
                if (JSONObject.parseObject(StrategyConfig().strategyConfig).keys.indexOf(item) >= 0){
                    strategy.add(item)
                }
            }
            preferenceUtils.setValue("strategy", strategy)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        updateData()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
    }
}


