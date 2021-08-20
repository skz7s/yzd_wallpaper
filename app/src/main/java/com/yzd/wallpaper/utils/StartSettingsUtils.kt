package com.yzd.wallpaper.utils

import android.content.Context
import android.content.Intent
import android.net.Uri


class StartSettingsUtils {

    fun getStartSettingIntent(packageName: String): Intent? {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        intent.data = Uri.fromParts("package", packageName, null)
        intent.component = null
        return intent
    }

    fun getAppDetail(context: Context): Intent {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        intent.data = Uri.fromParts("package", context.packageName, null)
        return intent
    }

}