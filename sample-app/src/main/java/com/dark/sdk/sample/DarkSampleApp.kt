package com.dark.sdk.sample

import android.app.Application
import android.util.Log
import com.dark.sdk.DarkConfig
import com.dark.sdk.DarkSdk
import com.dark.sdk.api.LogLevel

/**
 * Sample Application demonstrating DARK SDK initialization
 */
class DarkSampleApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize DARK SDK with custom configuration
        val config = DarkConfig(
            apiEndpoint = "https://api.dark-sdk.dev/v1",
            enableDaemon = true,
            enableRootHide = true,
            enableXposedHide = true,
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        )
        
        val success = DarkSdk.initialize(this, config)
        if (success) {
            Log.i("DarkSampleApp", "DARK SDK initialized successfully v${DarkSdk.getVersion()}")
        } else {
            Log.e("DarkSampleApp", "DARK SDK initialization failed")
        }
    }
    
    override fun onTerminate() {
        DarkSdk.shutdown()
        Log.i("DarkSampleApp", "DARK SDK shutdown")
        super.onTerminate()
    }
}