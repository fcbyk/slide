package com.fcbyk.slide.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

object ApiClient {

    // 持久化 Cookie，保持服务端 Session
    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
        })
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ===================== 认证相关 =====================

    /** 密码登录 */
    suspend fun login(serverUrl: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply { put("password", password) }
        val loginUrl = "$serverUrl/api/login"
        Log.d("ApiClient", "POST $loginUrl")

        val request = Request.Builder()
            .url(loginUrl)
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()
        executeRequest(request)
    }

    /** 扫码自动登录 — 服务端返回 HTML，只判断 HTTP 状态码 */
    suspend fun autoLogin(serverUrl: String, token: String): LoginResult = withContext(Dispatchers.IO) {
        val loginUrl = "$serverUrl/auto-login?token=${token}"
        Log.d("ApiClient", "GET $loginUrl")

        val request = Request.Builder()
            .url(loginUrl)
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            Log.d("ApiClient", "Auto-login response: ${response.code}")
            // 服务端返回 HTML 页面，Cookie 已由 CookieJar 自动保存
            if (response.isSuccessful) {
                LoginResult.Success
            } else {
                LoginResult.Error("二维码已过期或无效")
            }
        } catch (e: UnknownHostException) {
            LoginResult.Error("无法连接服务器\n请确认 IP 地址正确")
        } catch (e: ConnectException) {
            LoginResult.Error("连接被拒绝\n请确认端口正确")
        } catch (e: SocketTimeoutException) {
            LoginResult.Error("连接超时\n请确认手机和服务器在同一 WiFi")
        } catch (e: Exception) {
            Log.e("ApiClient", "Auto-login error: ${e.javaClass.simpleName}", e)
            LoginResult.Error("网络错误 [${e.javaClass.simpleName}]\n${e.message ?: ""}")
        }
    }

    /** 检查认证状态，同时测量延迟 */
    suspend fun checkAuth(serverUrl: String): ApiCallResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder().url("$serverUrl/api/check_auth").get().build()
            val response = client.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - start
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext ApiCallResult(false, elapsed)
                val json = JSONObject(body)
                val authenticated = json.optJSONObject("data")?.optBoolean("authenticated") ?: false
                ApiCallResult(authenticated, elapsed)
            } else {
                ApiCallResult(false, elapsed)
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            ApiCallResult(false, elapsed)
        }
    }

    /** 退出登录 */
    suspend fun logout(serverUrl: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/api/logout")
                .post("{}".toRequestBody(JSON))
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {
            // 静默处理
        }
    }

    // ===================== PPT 控制 =====================

    /** 下一页 */
    suspend fun nextSlide(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$serverUrl/api/next").post("{}".toRequestBody(JSON)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    /** 上一页 */
    suspend fun prevSlide(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$serverUrl/api/prev").post("{}".toRequestBody(JSON)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    // ===================== 鼠标控制 =====================

    /** 鼠标左键点击 */
    suspend fun mouseClick(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$serverUrl/api/mouse/click").post("{}".toRequestBody(JSON)).build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    // ===================== 内部方法 =====================

    private fun executeRequest(request: Request): LoginResult = try {
        val response = client.newCall(request).execute()
        Log.d("ApiClient", "Response code: ${response.code}")
        val body = response.body?.string() ?: return LoginResult.Error("服务器无响应")
        val json = JSONObject(body)
        if (response.isSuccessful && json.optInt("code") == 200) {
            LoginResult.Success
        } else {
            val msg = json.optString("message", "")
            val errorMsg = when {
                msg.lowercase().contains("password") || msg.lowercase().contains("invalid") -> "密码错误"
                msg.lowercase().contains("token") -> "二维码已过期"
                msg.isNotEmpty() -> msg
                else -> "登录失败 (${response.code})"
            }
            LoginResult.Error(errorMsg)
        }
    } catch (e: UnknownHostException) {
        Log.e("ApiClient", "UnknownHost", e)
        LoginResult.Error("无法连接服务器\n请确认 IP 地址正确且服务器在运行")
    } catch (e: ConnectException) {
        Log.e("ApiClient", "Connect refused", e)
        LoginResult.Error("连接被拒绝\n请确认端口正确")
    } catch (e: SocketTimeoutException) {
        Log.e("ApiClient", "Timeout", e)
        LoginResult.Error("连接超时\n请确认手机和服务器在同一 WiFi")
    } catch (e: SSLHandshakeException) {
        Log.e("ApiClient", "SSL handshake failed", e)
        LoginResult.Error("SSL 证书错误\n请使用 http:// 而非 https://")
    } catch (e: SSLPeerUnverifiedException) {
        Log.e("ApiClient", "SSL peer unverified", e)
        LoginResult.Error("SSL 证书不受信任\n请使用 http:// 开头")
    } catch (e: SSLException) {
        Log.e("ApiClient", "SSL error", e)
        LoginResult.Error("安全连接失败\n${e.message}")
    } catch (e: Exception) {
        Log.e("ApiClient", "Unexpected error: ${e.javaClass.simpleName}", e)
        LoginResult.Error("网络错误 [${e.javaClass.simpleName}]\n${e.message ?: ""}")
    }
}

sealed class LoginResult {
    data object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
}

data class ApiCallResult(
    val success: Boolean,
    val latency: Long, // ms
)
