package com.yzd.wallpaper


import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson.JSONObject
import com.obsez.android.lib.filechooser.ChooserDialog
import com.yzd.wallpaper.services.AutoService
import com.yzd.wallpaper.services.NotificationService
import com.yzd.wallpaper.utils.PreferenceUtils
import com.yzd.wallpaper.utils.StartSettingsUtils
import java.io.File


class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    private fun isNumeric(str: String): Boolean {
        for (i in str.indices) {
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }

    private fun initUpdate() {
        preferenceManager.findPreference<Preference>(
            "update"
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { _ ->
                val intent = Intent(activity, AutoService::class.java)
                intent.putExtra("operate", "UpdateWallpaper")
                activity?.startService(intent)
                Toast.makeText(activity, "开始更新", Toast.LENGTH_SHORT).show()
                false
            };
    }

    private fun initCollect() {
        preferenceManager.findPreference<Preference>(
            "collect"
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { _ ->
                val intent = Intent(activity, AutoService::class.java)
                intent.putExtra("operate", "CollectWallpaper")
                activity?.startService(intent)
                false
            };
    }

    private fun setIsServiceOpen() {
        preferenceManager.findPreference<Preference>(
            "is_service_open"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                if (newValue as Boolean) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.startForegroundService(
                            Intent(
                                activity,
                                NotificationService::class.java
                            )
                        )
                    } else {
                        activity?.startService(Intent(activity, NotificationService::class.java))
                    }
                    Toast.makeText(activity, "服务已开启", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(activity, NotificationService::class.java)
                    activity?.stopService(intent)
                    Toast.makeText(activity, "服务已关闭", Toast.LENGTH_SHORT).show()
                }
                true
            };
    }

    private fun setIsLocalPath() {
        setVisibleByIsLocalPath(PreferenceUtils(activity).isLocalPath)
        preferenceManager.findPreference<Preference>(
            "is_local_path"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                setVisibleByIsLocalPath(newValue as Boolean)
                true
            };
    }

    private fun setVisibleByIsLocalPath(isLocalPath: Boolean) {

        if (isLocalPath) {
            preferenceManager.findPreference<Preference>(
                "custom_path"
            )?.isVisible = true
            preferenceManager.findPreference<Preference>(
                "wallpaper_lib"
            )?.isVisible = false
            preferenceManager.findPreference<Preference>(
                "update_config"
            )?.isVisible = false
            preferenceManager.findPreference<Preference>(
                "total_num"
            )?.isVisible = false
        } else {
            preferenceManager.findPreference<Preference>(
                "custom_path"
            )?.isVisible = false
            preferenceManager.findPreference<Preference>(
                "wallpaper_lib"
            )?.isVisible = true
            preferenceManager.findPreference<Preference>(
                "update_config"
            )?.isVisible = true
            preferenceManager.findPreference<Preference>(
                "strategy"
            )?.isVisible = true
            preferenceManager.findPreference<Preference>(
                "total_num"
            )?.isVisible = true
        }

    }

    private fun initWallpaperLib(): Array<CharSequence> {
        val wallpaperPath: File = activity?.getExternalFilesDir("wallpaper") as File
        if (wallpaperPath.exists()) {
            var entries = arrayOf<CharSequence>("我的收藏")
            for (item in wallpaperPath.listFiles().filter { it.isDirectory && it.name != "我的收藏" }) {
                entries = entries.copyOf(entries.size + 1) as Array<CharSequence>
                entries[entries.lastIndex] = item.name
            }
            return entries
        }
        return arrayOf<CharSequence>()
    }

    private fun setWallpaperLib() {
        val wallpaperLib = preferenceManager.findPreference<MultiSelectListPreference>(
            "wallpaper_lib"
        )

        if (wallpaperLib != null) {
            val entries = initWallpaperLib()
            val libSet = PreferenceUtils(activity).defaultPrefs.getStringSet(
                "wallpaper_lib",
                mutableSetOf()
            )
            if (libSet != null) {
                if (libSet.isNotEmpty()) {
                    wallpaperLib?.summary = libSet.toString()
                } else {
                    wallpaperLib?.summary = "全随机"
                }
            }

            wallpaperLib?.entries = entries
            wallpaperLib?.entryValues = entries
        }

        wallpaperLib?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { preference, newValue ->
                if (newValue != null) {
                    if ((newValue as MutableSet<String>).isNotEmpty()) {
                        preference?.summary = newValue.toString()
                    } else {
                        preference?.summary = "全随机"
                    }
                }
                true
            }

        wallpaperLib?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { preference ->
                val wallpaperEntries = initWallpaperLib()
                (preference as MultiSelectListPreference).entries = wallpaperEntries
                preference.entryValues = wallpaperEntries
                true
            }
    }

    private fun setCustomPath() {
        val customPath = preferenceManager.findPreference<Preference>(
            "custom_path"
        )

        val customPathString = PreferenceUtils(activity).customPath
        if (customPathString != "") {
            customPath?.summary = customPathString.toString()
        } else {
            customPath?.summary = "为空不生效"
        }

        customPath?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { preference ->
                ChooserDialog(activity)
                    .withFilter(true, false)
                    .withChosenListener(ChooserDialog.Result { path, _ ->
                        if (path != null && path != "") {
                            val prefsWriter =
                                PreferenceUtils(activity).defaultPrefs.edit()
                            prefsWriter?.putString("custom_path", path)
                            prefsWriter?.apply()
                            preference?.summary = path
                        }
                    })
                    .build()
                    .show()

                true
            }
    }


    private fun setAutoUpdate() {
        preferenceManager.findPreference<Preference>(
            "is_auto_update"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                val intent = Intent(activity, NotificationService::class.java)
                activity?.startService(intent)
                true
            }
    }

    private fun setUpdateFreq() {
        preferenceManager.findPreference<EditTextPreference>(
            "update_freq"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { preference, newValue ->
                if (isNumeric(newValue.toString())) {
                    val num = Integer.parseInt(newValue.toString())
                    if (num < 1) {
                        preference.summary = "24"
                        Toast.makeText(activity, "更新频率不能为0", Toast.LENGTH_SHORT).show()
                        false
                    }
                    true
                } else {
                    preference.summary = "24"
                    Toast.makeText(activity, "请输入整数", Toast.LENGTH_SHORT).show()
                    false
                }

            }
    }

    private fun setStrategy() {
        val strategyPreference = preferenceManager.findPreference<MultiSelectListPreference>(
            "strategy"
        )
        val strategyConfig = JSONObject.parseObject(StrategyConfig().strategyConfig)

        if (strategyPreference != null) {
            val strategySet = PreferenceUtils(activity).strategy
            if (strategySet != null) {
                if (strategySet.isNotEmpty()) {
                    strategyPreference.summary = strategySet.toString()
                } else {
                    strategyPreference.summary = "全随机"
                }
                strategyPreference.entries = strategyConfig.keys.toTypedArray()
                strategyPreference.entryValues = strategyConfig.keys.toTypedArray()
            }
        }

        strategyPreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { preference, newValue ->
                if (newValue != null) {
                    if ((newValue as MutableSet<String>).isNotEmpty()) {
                        preference.summary = newValue.toString()
                    } else {
                        preference.summary = "全随机"
                    }
                }
                true
            }
    }

    private fun setTotalNum() {
        /*每个策略最大图片数*/
        preferenceManager.findPreference<EditTextPreference>(
            "total_num"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                if (isNumeric(newValue.toString())) {
                    val num = Integer.parseInt(newValue.toString())
                    if (num < 1) {
                        Toast.makeText(activity, "每个策略最少保留1张图片", Toast.LENGTH_SHORT).show()
                        false
                    } else if (num > 100) {
                        Toast.makeText(activity, "每个策略最多保留100张图片(避免大量存储)", Toast.LENGTH_SHORT)
                            .show()
                        false
                    }
                    true
                } else {
                    Toast.makeText(activity, "请输入整数", Toast.LENGTH_SHORT).show()
                    false
                }
            }
    }

    private fun setGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                val writer = PreferenceUtils(activity).defaultPrefs.edit()
                writer.putBoolean("is_gesture", false)
                writer.apply()
            }
        }
        preferenceManager.findPreference<Preference>(
            "is_gesture"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                if (newValue as Boolean) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.canDrawOverlays(activity)) {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + activity?.packageName)
                                )
                            )
                        }
                    }
                }
                val intent = Intent(activity, NotificationService::class.java)
                intent.putExtra("is_update", true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity?.startForegroundService(intent)
                } else {
                    activity?.startService(intent)
                }
                true
            }
    }

    private fun setHideTask() {
        if (PreferenceUtils(activity).isHideTask) {
            val am = activity?.getSystemService(Context.ACTIVITY_SERVICE)
            if (am != null) {
                val tasks = (am as ActivityManager).appTasks
                if (tasks.isNotEmpty()) {
                    tasks[0].setExcludeFromRecents(true)
                }
            }
        }

        preferenceManager.findPreference<Preference>(
            "hide_task"
        )?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener() { _, newValue ->
                if (newValue as Boolean) {
                    val am = activity?.getSystemService(Context.ACTIVITY_SERVICE)
                    if (am != null) {
                        val tasks = (am as ActivityManager).appTasks
                        if (tasks.isNotEmpty()) {
                            tasks[0].setExcludeFromRecents(true)
                        }
                    }
                }
                true
            }
    }

    private fun setPermissions() {
        preferenceManager.findPreference<Preference>(
            "permissions"
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { _ ->
                try {
                    val intent = activity?.let {
                        StartSettingsUtils()
                            .getAppDetail(it)
                    }
                    activity?.startActivity(intent);
                } catch (e: java.lang.Exception) {
                    val intent = activity?.packageName?.let {
                        StartSettingsUtils().getStartSettingIntent(
                            it
                        )
                    }
                    activity?.startActivity(intent);
                }

                false
            };
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        /* 操作 */
        initUpdate()
        initCollect()

        /* 全局配置 */
        setIsServiceOpen()
        setIsLocalPath()
        setWallpaperLib()
        setCustomPath()

        /* 网络壁纸库 */
        setAutoUpdate()
        setUpdateFreq()
        setStrategy()

        /* 其他 */
        setTotalNum()
        setGesture()
        setHideTask()
        setPermissions()

        if (PreferenceUtils(activity).defaultPrefs.getBoolean("is_service_open", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.startForegroundService(Intent(activity, NotificationService::class.java))
            } else {
                activity?.startService(Intent(activity, NotificationService::class.java))
            }
        }
    }
}