package com.yzd.wallpaper.operate

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.preference.PreferenceManager
import com.alibaba.fastjson.JSONObject
import com.yzd.wallpaper.utils.PersistentCookieStore
import com.zhy.http.okhttp.OkHttpUtils
import com.zhy.http.okhttp.callback.FileCallBack
import com.zhy.http.okhttp.callback.StringCallback
import okhttp3.Call
import okhttp3.HttpUrl
import org.htmlcleaner.HtmlCleaner
import java.io.File
import java.security.MessageDigest

class UpdateWallpaper(context: Context) {
    private val context: Context = context
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val isServerOpen = prefs.getBoolean("is_service_open", false)
    private val strategy = prefs.getString("strategy_conf", "")
    private val wallpaperPath: String
    private val wallpaperTemp: String
    private val strategyName = "update_lib"


    companion object {
        private const val TAG = "UpdateWallpaper"
    }

    init {
        val file = context.getExternalFilesDir("wallpaper")
        if (!file?.exists()!!) {
            file.mkdirs()
        }
        val fileTemp = context.getExternalFilesDir("temp")
        if (!fileTemp?.exists()!!) {
            fileTemp.mkdirs()
        }
        wallpaperPath = file.path
        wallpaperTemp = fileTemp.path
    }

    private fun isOn(): Boolean {
        if (!isServerOpen) {
            return false
        }
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager != null) {
            return wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
        }
        return false
    }

    fun operate() {
        val jsonStrategy = JSONObject.parseArray(strategy)

        val randomIndex = (Math.random() * (jsonStrategy?.size ?: 0)).toInt()
        var tempIndex = 0
        for (item in jsonStrategy) {
            if (randomIndex == tempIndex) {
                login(item as JSONObject)
                break
            }
            tempIndex++
        }
    }

    fun autoOperate() {
        if (isOn()) {
            operate()
        }
    }

    fun getMap(crawlMap: JSONObject): HashMap<String, String> {
        val resMap = HashMap<String, String>()
        for ((key, value) in crawlMap) {
            resMap[key] = value.toString()
        }
        return resMap
    }

    private fun getMap(crawlMap: JSONObject, page: String): HashMap<String, String> {
        val resMap = HashMap<String, String>()
        for ((key, value) in crawlMap) {
            if (value.toString() == "{page}" && page != "") {
                resMap[key] = page
            } else {
                resMap[key] = value.toString()
            }
        }
        return resMap
    }

    private fun login(allCrawl: JSONObject) {

        if (allCrawl.getJSONObject("login") != null) {
            val crawl = allCrawl.getJSONObject("login")
            val cookieStore =
                PersistentCookieStore(context)
            val lastCookies = cookieStore.get(HttpUrl.parse(crawl.getString("url").toString()))
            if (lastCookies.isNotEmpty()) {
                Log.i(TAG, "会话未过期，继续使用")
                crawlFunc(allCrawl.getJSONObject("crawl"), "")
            } else {
                Log.i(TAG, "登录")
                val request = OkHttpUtils.get()

                // 进入登录页面， 获取token等各种信息
                if (crawl.getJSONObject("headers") != null) {
                    Log.d(TAG, "headers: " + crawl.getJSONObject("headers").toJSONString())
                    request.headers(getMap(crawl.getJSONObject("headers")))
                }

                if (crawl.getJSONObject("data") != null) {
                    Log.d(TAG, "data: " + crawl.getJSONObject("data").toJSONString())
                    request.params(getMap(crawl.getJSONObject("data")))
                }

                request.url(
                    crawl.getString("url")
                ).build()
                    .execute(
                        object : StringCallback() {
                            override fun onResponse(response: String?, id: Int) {
                                val htmlCleaner = HtmlCleaner()
                                val tree = htmlCleaner.clean(response)

                                if (crawl.getString("login") != null) {
                                    val postRequest = OkHttpUtils.post()
                                    val crawl = crawl.getJSONObject("login")

                                    if (crawl.getJSONObject("headers") != null) {
                                        Log.d(
                                            TAG,
                                            "headers: " + crawl.getJSONObject("headers")
                                                .toJSONString()
                                        )
                                        postRequest.headers(getMap(crawl.getJSONObject("headers")))
                                    }

                                    val data = HashMap<String, String>()

                                    for ((key, value) in crawl.getJSONObject("data")) {
                                        if (value is String) {
                                            data[key] = value.toString()
                                        } else {
                                            for (tempData in tree.evaluateXPath(
                                                (value as JSONObject).getString("xpath")
                                                    .toString()
                                            )) {
                                                data[key] = tempData.toString()
                                            }
                                        }
                                    }

                                    postRequest.params(data)

                                    postRequest.url(
                                        crawl.getString("url")
                                    ).build()
                                        .execute(object : StringCallback() {
                                            override fun onResponse(response: String?, id: Int) {
                                                crawlFunc(
                                                    allCrawl.getJSONObject("crawl"),
                                                    ""
                                                )
                                            }

                                            override fun onError(
                                                call: Call?,
                                                e: Exception?,
                                                id: Int
                                            ) {
                                                Log.e(TAG, e.toString())
                                                crawlFunc(
                                                    allCrawl.getJSONObject("crawl"),
                                                    ""
                                                )
                                            }
                                        })
                                }
                            }

                            override fun onError(call: Call?, e: Exception?, id: Int) {
                                Log.e(TAG, e.toString())
                                crawlFunc(
                                    allCrawl.getJSONObject("crawl"),
                                    ""
                                )
                            }
                        })
            }
        } else {
            Log.d(TAG, "cookie未过期")
            crawlFunc(allCrawl.getJSONObject("crawl"), "")
        }
    }

    fun crawlFunc(crawl: JSONObject, response: String) {
        if (response == "") {
            Log.i(TAG, "获取数据")
        }

        var request = OkHttpUtils.get()

        Log.v(TAG, crawl.toJSONString())

        if (crawl.getJSONObject("headers") != null) {
            request.headers(getMap(crawl.getJSONObject("headers")))
        }

        var maxPage = 0

        if (crawl.getInteger("max_page") != null) {
            maxPage = crawl.getInteger("max_page")
        }

        if (crawl.getJSONObject("data") != null) {
            request.params(
                getMap(
                    crawl.getJSONObject("data"),
                    (Math.random() * maxPage).toInt().toString()
                )
            )
        }

        if (crawl.getString("url") != null) {
            if (response != "" && crawl.getJSONObject("url").getString("xpath") != null) {
                val htmlCleaner = HtmlCleaner()
                var tree = htmlCleaner.clean(response)

                for (url in tree.evaluateXPath(
                    crawl.getJSONObject("url").getString("xpath")
                )) {
                    request = request.url(
                        url.toString()
                    )
                    request.build()
                        .execute(object : StringCallback() {
                            override fun onResponse(response: String?, id: Int) {
                                if (crawl.getString("crawl") != null) {
                                    crawlFunc(
                                        crawl.getJSONObject("crawl"),
                                        response.toString()
                                    )
                                } else if (crawl.getString("result") != null) {
                                    val htmlCleaner = HtmlCleaner()
                                    var tn = htmlCleaner.clean(response)
                                    for (image in tn.evaluateXPath(
                                        crawl.getJSONObject("result").getJSONObject("url")
                                            .getString("xpath")
                                    )) {
                                        downloadImage(image.toString())
                                    }
                                }
                            }

                            override fun onError(call: Call?, e: Exception?, id: Int) {
                                Log.e(TAG, e.toString())
                            }
                        })
                }
            } else {
                request = request.url(
                    crawl.getString("url").replace(
                        "{page}", (Math.random() * maxPage).toInt().toString()
                    )
                )
                request.build()
                    .execute(object : StringCallback() {
                        override fun onResponse(response: String?, id: Int) {

                            if (crawl.getString("crawl") != null) {
                                crawlFunc(
                                    crawl.getJSONObject("crawl"),
                                    response.toString()
                                )
                            } else if (crawl.getString("result") != null) {
                                val htmlCleaner = HtmlCleaner()
                                val tn = htmlCleaner.clean(response)
                                for (image in tn.evaluateXPath(
                                    crawl.getJSONObject("result").getJSONObject("url")
                                        .getString("xpath")
                                )) {
                                    downloadImage(image.toString())
                                }
                            }
                        }

                        override fun onError(call: Call?, e: Exception?, id: Int) {
                            Log.e(TAG, e.toString())
                        }
                    })
            }
        }
    }

    private fun md5(content: String): String {
        val hash = MessageDigest.getInstance("MD5").digest(content.toByteArray())
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            var str = Integer.toHexString(b.toInt())
            if (b < 0x10) {
                str = "0$str"
            }
            hex.append(str.substring(str.length - 2))
        }
        return hex.toString()
    }

    fun downloadImage(url: String) {
        val strList = url.split(".").toTypedArray()
        var filename = this.md5(url) + "." + strList[strList.lastIndex]
        val targetFile =
            File(wallpaperPath + File.separator + strategyName + File.separator + filename)
        if (!targetFile.exists()) {
            OkHttpUtils
                .get()
                .url(url)
                .build()
                .execute(object : FileCallBack(
                    wallpaperTemp,
                    filename
                ) {
                    override fun onResponse(response: File?, id: Int) {
                        Log.d(TAG, File(wallpaperPath + "/" + response?.name).path)
                        val targetDir = File(wallpaperPath + File.separator + strategyName)
                        if (!targetDir.exists()) {
                            targetDir.mkdirs()
                        }

                        if (!targetFile.exists()) {
                            response?.renameTo(targetFile)
                        }
                    }

                    override fun onError(call: Call?, e: Exception?, id: Int) {
                        Log.e(TAG, e.toString())
                    }

                })
        }
    }
}