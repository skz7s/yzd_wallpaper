package com.yzd.wallpaper.operate

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.preference.PreferenceManager
import com.alibaba.fastjson.JSONObject
import com.yzd.wallpaper.StrategyConfig
import com.yzd.wallpaper.utils.PersistentCookieStore
import com.yzd.wallpaper.utils.PreferenceUtils
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
    private val preferenceUtils = PreferenceUtils(context)
    private val wallpaperPath: String
    private val wallpaperTemp: String
    private val defaultHeaders = HashMap<String, String>()

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

        defaultHeaders["user-agent"] =
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Mobile Safari/537.36"
        defaultHeaders["accept-language"] = "zh-CN,zh;q=0.9"
    }

    companion object {
        private const val TAG = "UpdateWallpaper"
    }

    private fun isOn(): Boolean {
        if (!preferenceUtils.isAutoUpdate || !preferenceUtils.isServiceOpen) {
            return false
        }
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager != null) {
            return wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
        }
        return false
    }

    fun operate(isAuto: Boolean) {
        if (!isAuto || isOn()) {
            val strategySet = preferenceUtils.strategy
            val strategyConfig = StrategyConfig().strategyConfig
            if (strategySet != null) {
                if (strategySet.isNotEmpty()) {
                    val randomIndex = (Math.random() * (strategySet?.size ?: 0)).toInt()
                    var tempIndex = 0
                    for (item in strategySet) {
                        if (randomIndex == tempIndex) {
                            login(
                                JSONObject.parseObject(strategyConfig).getJSONObject(item),
                                item
                            )
                            break
                        }
                        tempIndex++
                    }
                } else {
                    val randomIndex = (Math.random() * (strategySet?.size ?: 0)).toInt()
                    var tempIndex = 0
                    for (item in JSONObject.parseObject(strategyConfig).keys) {
                        if (randomIndex == tempIndex) {
                            login(
                                JSONObject.parseObject(strategyConfig).getJSONObject(item),
                                item
                            )
                            break
                        }
                        tempIndex++
                    }
                }
            }

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

    private fun login(allCrawl: JSONObject, strategy: String) {
        Log.d(TAG, strategy)
        if (allCrawl.getJSONObject("login") != null) {
            val crawl = allCrawl.getJSONObject("login")
            val cookieStore =
                PersistentCookieStore(context)
            val lastCookies = cookieStore.get(HttpUrl.parse(crawl.getString("url").toString()))
            if (lastCookies.isNotEmpty()) {
                Log.i(TAG, "会话未过期，继续使用")
                crawlFunc(allCrawl.getJSONObject("crawl"), "", strategy)
            } else {
                Log.i(TAG, "登录")
                val request = OkHttpUtils.get()

                // 进入登录页面， 获取token等各种信息
                if (crawl.getJSONObject("headers") != null) {
                    Log.d(TAG, "headers: " + crawl.getJSONObject("headers").toJSONString())
                    request.headers(getMap(crawl.getJSONObject("headers")))
                } else {
                    request.headers(defaultHeaders)
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
                                    } else {
                                        postRequest.headers(defaultHeaders)
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
                                                    "",
                                                    strategy
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
                                                    "",
                                                    strategy
                                                )
                                            }
                                        })
                                }
                            }

                            override fun onError(call: Call?, e: Exception?, id: Int) {
                                Log.e(TAG, e.toString())
                                crawlFunc(
                                    allCrawl.getJSONObject("crawl"),
                                    "",
                                    strategy
                                )
                            }
                        })
            }
        } else {
            Log.d(TAG, "无需登录")
            crawlFunc(allCrawl.getJSONObject("crawl"), "", strategy)
        }
    }

    fun crawlFunc(crawl: JSONObject, response: String, strategy: String) {
        if (response == "") {
            Log.i(TAG, "获取数据")
        }
        Log.v(TAG, crawl.toString())
        var request = OkHttpUtils.get()

        if (crawl.getJSONObject("headers") != null) {
            request.headers(getMap(crawl.getJSONObject("headers")))
        } else {
            request.headers(defaultHeaders)
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
                                        response.toString(),
                                        strategy
                                    )
                                } else if (crawl.getString("result") != null) {
                                    val htmlCleaner = HtmlCleaner()
                                    var tn = htmlCleaner.clean(response)
                                    for (image in tn.evaluateXPath(
                                        crawl.getJSONObject("result").getJSONObject("url")
                                            .getString("xpath")
                                    )) {
                                        downloadImage(image.toString(), strategy)
                                    }
                                }
                            }

                            override fun onError(call: Call?, e: Exception?, id: Int) {
                                Log.e(TAG, e.toString())
                            }
                        })
                }
            } else {
                Log.d(TAG, crawl.getString("url"))
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
                                    response.toString(),
                                    strategy
                                )
                            } else if (crawl.getString("result") != null) {
                                val htmlCleaner = HtmlCleaner()
                                val tn = htmlCleaner.clean(response)
                                for (image in tn.evaluateXPath(
                                    crawl.getJSONObject("result").getJSONObject("url")
                                        .getString("xpath")
                                )) {
                                    downloadImage(image.toString(), strategy)
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

    fun downloadImage(url: String, strategy: String) {
        val strList = url.split(".").toTypedArray()
        var filename = this.md5(url) + "." + strList[strList.lastIndex]
        val targetFile =
            File(wallpaperPath + File.separator + strategy + File.separator + filename)
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
                        val targetDir = File(wallpaperPath + File.separator + strategy)
                        if (!targetDir.exists()) {
                            targetDir.mkdirs()
                        }

                        if (!targetFile.exists()) {
                            response?.renameTo(targetFile)
                            deleteMore(strategy)
                        }
                    }

                    override fun onError(call: Call?, e: Exception?, id: Int) {
                        Log.e(TAG, e.toString())
                    }

                })
        }
    }

    fun deleteMore(strategy: String) {
        val totalNum = Integer.parseInt(preferenceUtils.totalNum)
        val targetDir =
            File(wallpaperPath + File.separator + strategy)
        var wallpaperList = targetDir.listFiles()

        if (wallpaperList.size > totalNum) {
            wallpaperList.sortBy {
                it.lastModified()
            }
            for (index in 0 until wallpaperList.size - totalNum) {
                wallpaperList[index].delete()
            }
        }
    }
}