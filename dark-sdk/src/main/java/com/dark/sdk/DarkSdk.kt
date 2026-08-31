package com.dark.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.dark.sdk.anogs.AnogsFix
import com.dark.sdk.api.*
import com.dark.sdk.auth.FacebookLoginFix
import com.dark.sdk.auth.TwitterLoginFix
import com.dark.sdk.audio.MicFix
import com.dark.sdk.core.CoreManager
import com.dark.sdk.core.DarkSdkInternal

/**
 * DARK SDK - Main Entry Point
 * 
 * Professional Android Virtualization & Sandbox SDK
 * 
 * Usage:
 * ```kotlin
 * // Initialize in Application class
 * DarkSdk.initialize(this, DarkConfig(
 *     apiEndpoint = "https://your-api.com/v1",
 *     enableDaemon = true,
 *     enableRootHide = true
 * ))
 * 
 * // Activate license
 * DarkSdk.activateLicense("USER_KEY") { status ->
 *     if (status.isActivated) {
 *         // Create virtual environment
 *         val env = DarkSdk.createEnvironment(EnvironmentConfig(
 *             packageName = "com.target.app"
 *         ))
 *         env.start { handle ->
 *             // Environment ready
 *         }
 *     }
 * }
 * ```
 */
object DarkSdk {
    
    @Volatile
    private var internal: DarkSdkInternal? = null
    
    /**
     * Initialize DARK SDK
     * Must be called before any other SDK operations
     * 
     * @param context Application context
     * @param config SDK configuration (optional, uses defaults if not provided)
     * @return true if initialization successful
     */
    fun initialize(context: Context, config: DarkConfig = DarkConfig()): Boolean {
        if (internal != null) {
            return true // Already initialized
        }
        
        internal = DarkSdkInternal.getInstance(context, config)
        return internal!!.initialize(config)
    }
    
    /**
     * Check if SDK is initialized and ready
     */
    fun isReady(): Boolean {
        return internal?.isReady() == true
    }
    
    /**
     * Get SDK version - Elite 2.0.0
     */
    fun getVersion(): String {
        return internal?.getVersion() ?: "2.0.0-ELITE"
    }

    /**
     * ELITE: Get current core type (D_CORE / Z_CORE / B_CORE)
     */
    fun getCoreType(): CoreManager.CoreType = CoreManager.getCurrent()
    fun getCoreInfo() = CoreManager.getInfo()
    fun switchCore(type: CoreManager.CoreType): CoreManager.CoreType = CoreManager.initialize(type)
    
    /**
     * Get current license status
     */
    fun getLicenseStatus(): LicenseStatus? {
        return internal?.getLicenseStatus()
    }
    
    /**
     * Activate license with user key
     * 
     * @param userKey License key from DARK SDK dashboard
     * @param callback Result callback
     */
    fun activateLicense(userKey: String, callback: LicenseCallback) {
        internal?.activateLicense(userKey, callback) 
            ?: callback.onFailure(LicenseError.NotInitialized("SDK not initialized. Call DarkSdk.initialize() first"))
    }
    
    /**
     * Create a new virtual environment
     * 
     * @param config Environment configuration
     * @return Environment handle for lifecycle management
     */
    fun createEnvironment(config: EnvironmentConfig): EnvironmentHandle? {
        return internal?.createEnvironment(config)
    }
    
    /**
     * Get all active environments
     */
    fun getActiveEnvironments(): List<EnvironmentHandle> {
        return internal?.getActiveEnvironments() ?: emptyList()
    }
    
    /**
     * Shutdown SDK and release all resources
     * Call this in Application.onTerminate() or when done
     */
    fun shutdown() {
        internal?.shutdown()
        internal = null
    }
    
    /**
     * Enable/disable debug logging
     */
    fun setLogLevel(level: LogLevel) {
        com.dark.sdk.utils.DarkLogger.setLevel(level)
    }

    // ================= ELITE FIXES =================

    /** Twitter Login Fix - call before login */
    fun loginWithTwitter(activity: Activity, config: TwitterLoginFix.TwitterConfig, cb: TwitterLoginFix.Callback) =
        TwitterLoginFix.login(activity, config, cb)
    fun handleTwitterCallback(intent: Intent?) = TwitterLoginFix.handleCallback(intent)

    /** Facebook Login Fix */
    fun loginWithFacebook(activity: Activity, config: FacebookLoginFix.FacebookConfig, cb: FacebookLoginFix.Callback) =
        FacebookLoginFix.login(activity, config, cb)
    fun handleFacebookCallback(intent: Intent?) = FacebookLoginFix.handleCallback(intent)
    fun initFacebook(context: Context, config: FacebookLoginFix.FacebookConfig) =
        FacebookLoginFix.initialize(context, config)

    /** Anogs Fix */
    fun applyAnogsFix(context: Context, cfg: AnogsFix.AnogsConfig = AnogsFix.AnogsConfig()) =
        AnogsFix.apply(context, cfg)
    fun loadAnogsLib(context: Context) = AnogsFix.loadAnogsLibrary(context)

    /** Mic Fix */
    fun applyMicFix(context: Context, cfg: MicFix.MicConfig = MicFix.MicConfig()) =
        MicFix.apply(context, cfg)
    fun testMic() = MicFix.testMic()
    fun setMicCommunicationMode(context: Context, enable: Boolean) = MicFix.setCommunicationMode(context, enable)

    /**
     * ELITE unified initialize - auto applies all fixes
     */
    fun initializeElite(context: Context, config: DarkConfig = DarkConfig(), core: CoreManager.CoreType = CoreManager.CoreType.AUTO): Boolean {
        CoreManager.initialize(core)
        // Auto-apply elite fixes
        try { AnogsFix.apply(context) } catch (_: Exception) {}
        try { MicFix.apply(context) } catch (_: Exception) {}
        return initialize(context, config)
    }
}