package com.dark.sdk.core

import android.content.Context
import android.util.Log
import com.dark.sdk.BuildConfig
import com.dark.sdk.api.*
import com.dark.sdk.core.CoreManager
import com.dark.sdk.internal.LicenseManager
import com.dark.sdk.internal.EnvironmentManager
import com.dark.sdk.utils.DarkLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Main DARK SDK implementation
 * Thread-safe singleton providing all virtualization capabilities
 */
class DarkSdkInternal private constructor(
    private val context: Context,
    private val config: DarkConfig
) : IDarkSdk {
    override fun asBinder(): android.os.IBinder = android.os.Binder()
    
    companion object {
        @Volatile
        private var instance: DarkSdkInternal? = null
        private val lock = Any()
        
        fun getInstance(context: Context, config: DarkConfig = DarkConfig()): DarkSdkInternal {
            return instance ?: synchronized(lock) {
                instance ?: DarkSdkInternal(context.applicationContext, config).also { instance = it }
            }
        }
        
        fun destroyInstance() {
            synchronized(lock) {
                instance?.shutdown()
                instance = null
            }
        }
    }
    
    private val logger = DarkLogger.getInstance(config.logLevel)
    private val licenseManager = LicenseManager(context, config)
    private val environmentManager = EnvironmentManager(context, config, licenseManager)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeEnvironments = ConcurrentHashMap<String, EnvironmentHandleImpl>()
    private var isInitialized = false
    private var isShutdown = false
    
    override fun initialize(config: DarkConfig): Boolean {
        if (isInitialized) {
            logger.w("SDK already initialized")
            return true
        }
        
        if (isShutdown) {
            logger.e("Cannot initialize after shutdown")
            return false
        }
        
        logger.i("Initializing DARK ELITE SDK v${getVersion()} core=${CoreManager.getCurrent()}")
        
        // ELITE: Auto-select core D/Z/B
        CoreManager.initialize()

        // ELITE FIX: Anogs
        try { com.dark.sdk.anogs.AnogsFix.apply(context) } catch (_: Exception) {}
        // ELITE FIX: Mic
        try { com.dark.sdk.audio.MicFix.apply(context) } catch (_: Exception) {}
        
        // Initialize license manager
        val licenseOk = licenseManager.initialize()
        if (!licenseOk) {
            logger.w("License manager initialization failed - some features may be limited")
        }
        
        // Load saved license status
        licenseManager.loadSavedStatus()
        
        // Initialize environment manager
        environmentManager.initialize()

        // ELITE: migrate B_CORE data
        try { CoreManager.migrateFromBCore(context) } catch (_: Exception) {}
        
        isInitialized = true
        logger.i("DARK ELITE SDK initialized successfully core=${CoreManager.getCurrent()} version=${getVersion()}")
        return true
    }
    
    override fun isReady(): Boolean {
        return isInitialized && !isShutdown && licenseManager.isLicensed()
    }
    
    override fun getVersion(): String = try { BuildConfig.VERSION_NAME } catch (_: Exception) { "2.0.0-ELITE" }
    
    override fun getLicenseStatus(): LicenseStatus {
        return licenseManager.getCurrentStatus()
    }
    
    override fun activateLicense(userKey: String, callback: LicenseCallback) {
        if (!isInitialized) {
            callback.onFailure(LicenseError.NotInitialized("SDK not initialized"))
            return
        }
        
        scope.launch {
            licenseManager.activate(userKey, object : LicenseManager.ActivationCallback {
                override fun onSuccess(status: LicenseStatus) {
                    callback.onSuccess(status)
                }
                
                override fun onFailure(error: LicenseError) {
                    callback.onFailure(error)
                }
            })
        }
    }
    
    override fun createEnvironment(config: EnvironmentConfig): EnvironmentHandle {
        val handle = EnvironmentHandleImpl(
            id = java.util.UUID.randomUUID().toString(),
            packageName = config.packageName,
            config = config,
            manager = environmentManager
        )
        activeEnvironments[handle.id] = handle
        return handle
    }
    
    override fun getActiveEnvironments(): List<EnvironmentHandle> {
        return activeEnvironments.values.filter { it.isRunning }.toList()
    }
    
    override fun shutdown() {
        logger.i("Shutting down DARK SDK")
        isShutdown = true
        
        // Stop all environments
        activeEnvironments.values.forEach { env ->
            try {
                if (env.isRunning) {
                    env.stop(object : EnvironmentCallback {
                        override fun onSuccess(handle: EnvironmentHandle) {}
                        override fun onFailure(error: EnvironmentError) {}
                    })
                }
            } catch (e: Exception) {
                logger.e("Error stopping environment ${env.id}", e)
            }
        }
        activeEnvironments.clear()
        
        // Shutdown managers
        environmentManager.shutdown()
        licenseManager.shutdown()
        scope.cancel()
        
        isInitialized = false
        logger.i("DARK SDK shutdown complete")
    }
    
    internal fun getContext(): Context = context
    internal fun getLogger() = logger
    internal fun getLicenseManager() = licenseManager
    internal fun getEnvironmentManager() = environmentManager
}

/**
 * Internal environment handle implementation
 */
class EnvironmentHandleImpl(
    override val id: String,
    override val packageName: String,
    private val config: EnvironmentConfig,
    private val manager: EnvironmentManager
) : EnvironmentHandle {
    
    @Volatile
    private var running = false
    @Volatile
    private var processId: Int? = null
    
    override val isRunning: Boolean
        get() = running
    
    override val pid: Int?
        get() = processId
    
    override fun start(callback: EnvironmentCallback) {
        manager.startEnvironment(this, config, object : EnvironmentManager.StartCallback {
            override fun onSuccess(pid: Int) {
                running = true
                processId = pid
                callback.onSuccess(this@EnvironmentHandleImpl)
            }
            
            override fun onFailure(error: EnvironmentError) {
                callback.onFailure(error)
            }
        })
    }
    
    override fun stop(callback: EnvironmentCallback) {
        manager.stopEnvironment(this, object : EnvironmentManager.StopCallback {
            override fun onSuccess() {
                running = false
                processId = null
                callback.onSuccess(this@EnvironmentHandleImpl)
            }
            
            override fun onFailure(error: EnvironmentError) {
                callback.onFailure(error)
            }
        })
    }
    
    override fun restart(callback: EnvironmentCallback) {
        stop(object : EnvironmentCallback {
            override fun onSuccess(handle: EnvironmentHandle) {
                start(callback)
            }
            override fun onFailure(error: EnvironmentError) {
                callback.onFailure(error)
            }
        })
    }
    
    override fun execute(command: String, callback: ExecutionCallback) {
        manager.executeCommand(this, command, callback)
    }
    
    override fun installApk(apkPath: String, callback: InstallationCallback) {
        manager.installApk(this, apkPath, callback)
    }
    
    override fun uninstallPackage(packageName: String, callback: UninstallCallback) {
        manager.uninstallPackage(this, packageName, callback)
    }
    
    override fun getStats(): EnvironmentStats {
        return manager.getStats(this)
    }
}