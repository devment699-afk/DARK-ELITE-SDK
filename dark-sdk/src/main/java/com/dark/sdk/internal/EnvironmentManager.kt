package com.dark.sdk.internal

import android.content.Context
import com.dark.sdk.api.*
import com.dark.sdk.utils.DarkLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages virtual environments lifecycle
 */
class EnvironmentManager(
    private val context: Context,
    private val config: DarkConfig,
    private val licenseManager: LicenseManager
) {
    
    private val logger = DarkLogger.getInstance(config.logLevel)
    private val environments = ConcurrentHashMap<String, EnvironmentState>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val pidCounter = AtomicInteger(10000)
    private var initialized = false
    
    interface StartCallback {
        fun onSuccess(pid: Int)
        fun onFailure(error: EnvironmentError)
    }
    
    interface StopCallback {
        fun onSuccess()
        fun onFailure(error: EnvironmentError)
    }
    
    fun initialize() {
        if (initialized) return
        logger.d("Initializing EnvironmentManager")
        initialized = true
    }
    
    fun startEnvironment(
        handle: EnvironmentHandleImpl,
        envConfig: EnvironmentConfig,
        callback: StartCallback
    ) {
        scope.launch {
            try {
                // Validate license
                if (!licenseManager.isLicensed()) {
                    callback.onFailure(EnvironmentError.PermissionDenied("License not valid"))
                    return@launch
                }
                
                // Check if already running
                val existing = environments[handle.id]
                if (existing?.isRunning == true) {
                    callback.onFailure(EnvironmentError.AlreadyRunning("Environment already running"))
                    return@launch
                }
                
                // Simulate environment creation (in real impl, this would start VM/container)
                val pid = pidCounter.incrementAndGet()
                
                val state = EnvironmentState(
                    handle = handle,
                    config = envConfig,
                    pid = pid,
                    startTime = System.currentTimeMillis()
                )
                environments[handle.id] = state
                
                logger.i("Started environment ${handle.id} for ${envConfig.packageName} (PID: $pid)")
                callback.onSuccess(pid)
                
            } catch (e: Exception) {
                logger.e("Failed to start environment", e)
                callback.onFailure(EnvironmentError.Unknown(e.message ?: "Unknown error"))
            }
        }
    }
    
    fun stopEnvironment(handle: EnvironmentHandleImpl, callback: StopCallback) {
        scope.launch {
            try {
                val state = environments.remove(handle.id)
                if (state == null) {
                    callback.onFailure(EnvironmentError.NotFound("Environment not found"))
                    return@launch
                }
                
                logger.i("Stopped environment ${handle.id}")
                callback.onSuccess()
                
            } catch (e: Exception) {
                logger.e("Failed to stop environment", e)
                callback.onFailure(EnvironmentError.Unknown(e.message ?: "Unknown error"))
            }
        }
    }
    
    fun executeCommand(handle: EnvironmentHandleImpl, command: String, callback: ExecutionCallback) {
        scope.launch {
            val state = environments[handle.id]
            if (state == null || !state.isRunning) {
                callback.onFailure(EnvironmentError.NotFound("Environment not running"))
                return@launch
            }
            
            // Simulate command execution
            val output = simulateCommand(command, state.config.packageName)
            callback.onSuccess(output, 0)
        }
    }
    
    fun installApk(handle: EnvironmentHandleImpl, apkPath: String, callback: InstallationCallback) {
        scope.launch {
            val state = environments[handle.id]
            if (state == null || !state.isRunning) {
                callback.onFailure(EnvironmentError.NotFound("Environment not running"))
                return@launch
            }
            
            // Simulate APK installation
            val packageName = extractPackageName(apkPath)
            logger.i("Installed $packageName in environment ${handle.id}")
            callback.onSuccess(packageName)
        }
    }
    
    fun uninstallPackage(handle: EnvironmentHandleImpl, packageName: String, callback: UninstallCallback) {
        scope.launch {
            val state = environments[handle.id]
            if (state == null || !state.isRunning) {
                callback.onFailure(EnvironmentError.NotFound("Environment not running"))
                return@launch
            }
            
            logger.i("Uninstalled $packageName from environment ${handle.id}")
            callback.onSuccess()
        }
    }
    
    fun getStats(handle: EnvironmentHandleImpl): EnvironmentStats {
        val state = environments[handle.id]
        if (state == null) {
            return EnvironmentStats(0, 0f, 0, 0, 0, 0)
        }
        
        val uptime = System.currentTimeMillis() - state.startTime
        // Simulate stats
        return EnvironmentStats(
            memoryUsageMb = (Math.random() * 200 + 50).toLong(),
            cpuUsagePercent = (Math.random() * 30 + 5).toFloat(),
            storageUsageMb = (Math.random() * 500 + 100).toLong(),
            networkRxBytes = (Math.random() * 10_000_000).toLong(),
            networkTxBytes = (Math.random() * 5_000_000).toLong(),
            uptimeMillis = uptime
        )
    }
    
    fun shutdown() {
        scope.cancel()
        environments.clear()
        initialized = false
    }
    
    private fun simulateCommand(command: String, packageName: String): String {
        return when {
            command.startsWith("pm list") -> "package:$packageName\npackage:com.android.systemui"
            command.startsWith("dumpsys meminfo") -> "** MEMINFO **\nTotal PSS: 150MB"
            command.startsWith("logcat") -> "D/$packageName: App started\nI/$packageName: Activity created"
            else -> "Executed: $command\nExit code: 0"
        }
    }
    
    private fun extractPackageName(apkPath: String): String {
        // In real implementation, use aapt or PackageManager
        return "com.example.${apkPath.hashCode().toString(36)}"
    }
    
    private data class EnvironmentState(
        val handle: EnvironmentHandleImpl,
        val config: EnvironmentConfig,
        val pid: Int,
        val startTime: Long
    ) {
        val isRunning: Boolean = true
    }
}