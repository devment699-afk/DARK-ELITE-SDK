package com.dark.sdk.utils

import android.content.Context
import android.provider.Settings.Secure

/**
 * Network and device utilities
 */
object NetworkUtils {
    
    fun getDeviceId(context: Context): String {
        return try {
            Secure.getString(context.contentResolver, Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            activeNetwork?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            when (activeNetwork?.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> "WIFI"
                android.net.ConnectivityManager.TYPE_MOBILE -> "MOBILE"
                android.net.ConnectivityManager.TYPE_ETHERNET -> "ETHERNET"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}