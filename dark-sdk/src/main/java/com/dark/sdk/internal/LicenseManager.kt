package com.dark.sdk.internal

import android.content.Context
import android.content.SharedPreferences
import com.dark.sdk.api.*
import com.dark.sdk.utils.DarkLogger
import com.dark.sdk.BuildConfig
import com.dark.sdk.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Handles license activation, validation, and management
 */
class LicenseManager(
    private val context: Context,
    private val config: DarkConfig
) {
    
    private val logger = DarkLogger.getInstance(config.logLevel)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val PREF_NAME = "dark_sdk_license"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_EXPIRY = "expiry"
        private const val KEY_FEATURE_DAEMON = "feature_daemon"
        private const val KEY_FEATURE_ROOT_HIDE = "feature_root_hide"
        private const val KEY_FEATURE_XPOSED_HIDE = "feature_xposed_hide"
        private const val KEY_FEATURE_CUSTOM1 = "feature_custom1"
        private const val KEY_FEATURE_CUSTOM2 = "feature_custom2"
        private const val KEY_SERVER_STATUS = "server_status"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }
    
    interface ActivationCallback {
        fun onSuccess(status: LicenseStatus)
        fun onFailure(error: LicenseError)
    }
    
    fun initialize(): Boolean {
        logger.d("Initializing LicenseManager")
        return true
    }
    
    fun loadSavedStatus() {
        val serverStatus = prefs.getString(KEY_SERVER_STATUS, "unknown")
        logger.d("Loaded server status: $serverStatus")
    }
    
    fun isLicensed(): Boolean {
        val activated = prefs.getBoolean(KEY_ACTIVATED, false)
        if (!activated) return false
        
        val expiryStr = prefs.getString(KEY_EXPIRY, "")
        if (!expiryStr.isNullOrEmpty()) {
            return try {
                val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val expiryDate = sdf.parse(expiryStr)
                expiryDate?.time ?: 0 > System.currentTimeMillis()
            } catch (e: Exception) {
                logger.w("Error parsing expiry date: ${e.message}")
                true
            }
        }
        return true
    }
    
    fun getCurrentStatus(): LicenseStatus {
        val activated = prefs.getBoolean(KEY_ACTIVATED, false)
        val expiryStr = prefs.getString(KEY_EXPIRY, null)
        val serverStatusStr = prefs.getString(KEY_SERVER_STATUS, "unknown")
        
        var daysRemaining = 0L
        if (expiryStr != null && expiryStr.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                val expiryDate = sdf.parse(expiryStr)
                if (expiryDate != null) {
                    val diff = expiryDate.time - System.currentTimeMillis()
                    daysRemaining = maxOf(0, TimeUnit.MILLISECONDS.toDays(diff))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        val features = LicenseFeatures(
            daemonEnabled = prefs.getBoolean(KEY_FEATURE_DAEMON, config.enableDaemon),
            rootHideEnabled = prefs.getBoolean(KEY_FEATURE_ROOT_HIDE, config.enableRootHide),
            xposedHideEnabled = prefs.getBoolean(KEY_FEATURE_XPOSED_HIDE, config.enableXposedHide),
            customFeature1 = prefs.getBoolean(KEY_FEATURE_CUSTOM1, false),
            customFeature2 = prefs.getBoolean(KEY_FEATURE_CUSTOM2, false)
        )
        
        val serverStatus = when ((serverStatusStr ?: "unknown").lowercase(Locale.ROOT)) {
            "online" -> ServerStatus.ONLINE
            "offline" -> ServerStatus.OFFLINE
            "maintenance" -> ServerStatus.MAINTENANCE
            else -> ServerStatus.UNKNOWN
        }
        
        return LicenseStatus(
            isActivated = activated,
            expiryDate = expiryStr,
            daysRemaining = daysRemaining,
            features = features,
            serverStatus = serverStatus
        )
    }
    
    fun activate(userKey: String, callback: ActivationCallback) {
        if (userKey.isBlank()) {
            callback.onFailure(LicenseError.InvalidKey("User key cannot be empty"))
            return
        }
        
        scope.launch {
            withContext(Dispatchers.IO) {
                performActivation(userKey, callback)
            }
        }
    }
    
    private fun performActivation(userKey: String, callback: ActivationCallback) {
        var retries = 0
        val maxRetries = 3
        
        while (retries <= maxRetries) {
            try {
                val response = sendActivationRequest(userKey)
                val json = JSONObject(response)
                
                val status = json.getString("status")
                
                if (status == "success") {
                    handleSuccessResponse(json)
                    val licenseStatus = getCurrentStatus()
                    callback.onSuccess(licenseStatus)
                    return
                } else {
                    val reason = json.optString("reason", "Unknown error")
                    val serverMode = json.optString("server_mode", "online")
                    
                    when (serverMode) {
                        "maintenance" -> {
                            saveServerStatus("maintenance")
                            callback.onFailure(LicenseError.Maintenance(reason))
                            return
                        }
                        "offline" -> {
                            saveServerStatus("offline")
                            callback.onFailure(LicenseError.ServerError(503, reason))
                            return
                        }
                    }
                    
                    callback.onFailure(LicenseError.ServerError(
                        json.optInt("code", 400), reason))
                    return
                }
                
            } catch (e: Exception) {
                retries++
                logger.w("Activation attempt $retries failed: ${e.message}")
                
                if (retries > maxRetries) {
                    callback.onFailure(LicenseError.NetworkError(
                        "Failed after $maxRetries attempts: ${e.message}"))
                    return
                }
                
                Thread.sleep(2000)
            }
        }
    }
    
    private fun sendActivationRequest(userKey: String): String {
        val url = URL(config.apiEndpoint + "/activate")
        val connection = url.openConnection() as HttpURLConnection
        
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "DarkSDK/2.0.0-ELITE")
        }
        
        val postData = "user_key=${URLEncoder.encode(userKey, "UTF-8")}" +
            "&package_name=${URLEncoder.encode(context.packageName, "UTF-8")}" +
            "&device_id=${URLEncoder.encode(NetworkUtils.getDeviceId(context), "UTF-8")}" +
            "&sdk_version=${URLEncoder.encode("2.0.0-ELITE", "UTF-8")}"
        
        connection.outputStream.use { it.write(postData.toByteArray()) }
        
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw RuntimeException("HTTP $responseCode: ${connection.responseMessage}")
        }
        
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
    
    private fun handleSuccessResponse(json: JSONObject) {
        val expiry = json.optString("expiry", "")
        val toggleDaemon = json.optInt("feature_daemon", 0)
        val toggleRootHide = json.optInt("feature_root_hide", 0)
        val toggleXposedHide = json.optInt("feature_xposed_hide", 0)
        val custom1 = json.optInt("feature_custom1", 0)
        val custom2 = json.optInt("feature_custom2", 0)
        
        prefs.edit().apply {
            putBoolean(KEY_ACTIVATED, true)
            if (expiry.isNotEmpty()) putString(KEY_EXPIRY, expiry)
            putBoolean(KEY_FEATURE_DAEMON, toggleDaemon == 1)
            putBoolean(KEY_FEATURE_ROOT_HIDE, toggleRootHide == 1)
            putBoolean(KEY_FEATURE_XPOSED_HIDE, toggleXposedHide == 1)
            putBoolean(KEY_FEATURE_CUSTOM1, custom1 == 1)
            putBoolean(KEY_FEATURE_CUSTOM2, custom2 == 1)
            putString(KEY_SERVER_STATUS, "online")
            apply()
        }
        
        logger.i("License activated successfully${if (expiry.isNotEmpty()) " until $expiry" else ""}")
    }
    
    private fun saveServerStatus(status: String) {
        prefs.edit().putString(KEY_SERVER_STATUS, status).apply()
    }
    
    fun shutdown() {
        scope.cancel()
    }
}