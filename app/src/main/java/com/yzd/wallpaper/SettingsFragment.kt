package com.yzd.wallpaper


import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.yzd.wallpaper.services.AutoService
import com.yzd.wallpaper.services.NotificationService
import com.yzd.wallpaper.utils.StartSettingsUtils
import java.io.File


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var defaultPrefs: SharedPreferences

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

    private fun setUpdate() {
        preferenceManager.findPreference<Preference>(
            "update"
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { _ ->
                val intent = Intent(activity, AutoService::class.java)
                intent.putExtra("operate", "UpdateWallpaper")
                activity?.startService(intent)
                false
            };
    }

    private fun showInfo() {
        val wallpaperPath = this.activity?.getExternalFilesDir("wallpaper") as File

        val customLibPath = File(wallpaperPath.path + "/custom_lib")

        if (!customLibPath.isDirectory) {
            customLibPath.mkdirs()
        }

        preferenceManager.findPreference<Preference>(
            "info"
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener() { _ ->
                activity?.let {
                    val builder = AlertDialog.Builder(it)
                    // Set other dialog properties
                    builder.setTitle("说明")
                    builder.setMessage(
                        "1，初次使用，请先配置策略而后更新壁纸或手动拷贝图片到壁纸目录（参考3和4）\n2，打开服务后，如果壁纸目录下有壁纸，将在每次亮屏前切换，也可以点击通知以切换。\n" +
                                "3，配置策略后，点击立即更新，会根据策略随机下载壁纸到壁纸目录，策略格式为jsonList，详情请参考https://kardel.xyz/blog/detail?aid=16\n" +
                                "4，可以通过文件管理器管理壁纸目录，比如删除自动下载的图片，比如，拷贝自己的图片至壁纸目录。" +
                                "壁纸目录为" + wallpaperPath.absolutePath + "，其中update_lib下存储自动更新的壁纸，用户可以将自己的图片拷贝进custom_lib目录下，即可自动切换\n"
                    )
                    builder.create()
                    builder.show()
                }
                false
            };


    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        /* 全局配置 */
        setIsServiceOpen()

        /* 更新 */
        setUpdate()

        /* 其他 */
        setPermissions()

        showInfo()

        if (defaultPrefs.getBoolean("is_service_open", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity?.startForegroundService(Intent(activity, NotificationService::class.java))
            } else {
                activity?.startService(Intent(activity, NotificationService::class.java))
            }
        }
    }
}