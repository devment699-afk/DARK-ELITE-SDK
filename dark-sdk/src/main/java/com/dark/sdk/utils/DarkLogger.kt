package com.dark.sdk.utils

import android.util.Log
import com.dark.sdk.api.DarkConfig.LogLevel

/**
 * Professional logging utility for DARK SDK
 */
class DarkLogger private constructor(private val minLevel: LogLevel) {
    
    companion object {
        @Volatile
        private var instance: DarkLogger? = null
        private val lock = Any()
        private const val TAG_PREFIX = "DARK-SDK"
        
        fun getInstance(level: LogLevel = LogLevel.INFO): DarkLogger {
            return instance ?: synchronized(lock) {
                instance ?: DarkLogger(level).also { instance = it }
            }
        }
        
        fun setLevel(level: LogLevel) {
            synchronized(lock) {
                instance = DarkLogger(level)
            }
        }
    }
    
    fun d(tag: String, message: String) {
        if (minLevel.ordinal <= LogLevel.DEBUG.ordinal) {
            Log.d("$TAG_PREFIX:$tag", message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (minLevel.ordinal <= LogLevel.INFO.ordinal) {
            Log.i("$TAG_PREFIX:$tag", message)
        }
    }
    
    fun w(tag: String, message: String) {
        if (minLevel.ordinal <= LogLevel.WARNING.ordinal) {
            Log.w("$TAG_PREFIX:$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (minLevel.ordinal <= LogLevel.ERROR.ordinal) {
            Log.e("$TAG_PREFIX:$tag", message, throwable)
        }
    }
    
    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        if (minLevel.ordinal <= LogLevel.ERROR.ordinal) {
            Log.wtf("$TAG_PREFIX:$tag", message, throwable)
        }
    }
    
    // Extension functions for cleaner usage
    fun d(message: String) = d("SDK", message)
    fun i(message: String) = i("SDK", message)
    fun w(message: String) = w("SDK", message)
    fun e(message: String, throwable: Throwable? = null) = e("SDK", message, throwable)
}