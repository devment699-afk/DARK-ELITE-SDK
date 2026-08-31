package com.dark.sdk.api

import android.os.IInterface

/**
 * Main entry point for DARK SDK.
 * Provides access to all virtualization and sandboxing features.
 */
interface IDarkSdk : IInterface {
    
    /**
     * Initialize the SDK with configuration
     */
    fun initialize(config: DarkConfig): Boolean
    
    /**
     * Check if SDK is properly initialized and licensed
     */
    fun isReady(): Boolean
    
    /**
     * Get current SDK version
     */
    fun getVersion(): String
    
    /**
     * Get license status
     */
    fun getLicenseStatus(): LicenseStatus
    
    /**
     * Activate license with user key
     */
    fun activateLicense(userKey: String, callback: LicenseCallback)
    
    /**
     * Create a new virtual environment
     */
    fun createEnvironment(config: EnvironmentConfig): EnvironmentHandle
    
    /**
     * Get active environments
     */
    fun getActiveEnvironments(): List<EnvironmentHandle>
    
    /**
     * Shutdown SDK and cleanup resources
     */
    fun shutdown()
}

/**
 * Configuration for SDK initialization
 */
data class DarkConfig(
    val apiEndpoint: String = "https://api.dark-sdk.dev/v1",
    val enableDaemon: Boolean = true,
    val enableRootHide: Boolean = true,
    val enableXposedHide: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val customUserAgent: String? = null
)

/**
 * Log levels for SDK
 */
enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, NONE
}

/**
 * License status information
 */
data class LicenseStatus(
    val isActivated: Boolean,
    val expiryDate: String?,
    val daysRemaining: Long,
    val features: LicenseFeatures,
    val serverStatus: ServerStatus
)

/**
 * Available license features
 */
data class LicenseFeatures(
    val daemonEnabled: Boolean,
    val rootHideEnabled: Boolean,
    val xposedHideEnabled: Boolean,
    val customFeature1: Boolean,
    val customFeature2: Boolean
)

/**
 * Server connection status
 */
enum class ServerStatus {
    ONLINE, OFFLINE, MAINTENANCE, UNKNOWN
}

/**
 * Callback for license operations
 */
interface LicenseCallback {
    fun onSuccess(status: LicenseStatus)
    fun onFailure(error: LicenseError)
}

/**
 * License error types
 */
sealed class LicenseError(
    val code: Int,
    val message: String
) {
    data class NetworkError(override val message: String) : LicenseError(-1, message)
    data class ServerError(override val code: Int, override val message: String) : LicenseError(code, message)
    data class InvalidKey(override val message: String) : LicenseError(400, message)
    data class Expired(override val message: String) : LicenseError(401, message)
    data class Maintenance(override val message: String) : LicenseError(503, message)
    data class Unknown(override val message: String) : LicenseError(-999, message)
}

/**
 * Virtual environment configuration
 */
data class EnvironmentConfig(
    val packageName: String,
    val displayName: String? = null,
    val icon: Int? = null,
    val enableRoot: Boolean = false,
    val enableXposed: Boolean = false,
    val customProps: Map<String, String> = emptyMap(),
    val resourceLimits: ResourceLimits = ResourceLimits()
)

/**
 * Resource limits for virtual environment
 */
data class ResourceLimits(
    val maxMemoryMb: Int = 512,
    val maxCpuPercent: Int = 80,
    val maxStorageMb: Int = 1024,
    val networkAllowed: Boolean = true
)

/**
 * Handle to a running virtual environment
 */
interface EnvironmentHandle {
    val id: String
    val packageName: String
    val isRunning: Boolean
    val pid: Int?
    
    fun start(callback: EnvironmentCallback)
    fun stop(callback: EnvironmentCallback)
    fun restart(callback: EnvironmentCallback)
    fun execute(command: String, callback: ExecutionCallback)
    fun installApk(apkPath: String, callback: InstallationCallback)
    fun uninstallPackage(packageName: String, callback: UninstallCallback)
    fun getStats(): EnvironmentStats
}

/**
 * Environment operation callback
 */
interface EnvironmentCallback {
    fun onSuccess(handle: EnvironmentHandle)
    fun onFailure(error: EnvironmentError)
}

/**
 * Command execution callback
 */
interface ExecutionCallback {
    fun onSuccess(output: String, exitCode: Int)
    fun onFailure(error: EnvironmentError)
}

/**
 * APK installation callback
 */
interface InstallationCallback {
    fun onSuccess(packageName: String)
    fun onFailure(error: EnvironmentError)
}

/**
 * Package uninstall callback
 */
interface UninstallCallback {
    fun onSuccess()
    fun onFailure(error: EnvironmentError)
}

/**
 * Environment runtime statistics
 */
data class EnvironmentStats(
    val memoryUsageMb: Long,
    val cpuUsagePercent: Float,
    val storageUsageMb: Long,
    val networkRxBytes: Long,
    val networkTxBytes: Long,
    val uptimeMillis: Long
)

/**
 * Environment error types
 */
sealed class EnvironmentError(
    val code: Int,
    val message: String
) {
    data class NotInitialized(override val message: String) : EnvironmentError(-1, message)
    data class NotFound(override val message: String) : EnvironmentError(404, message)
    data class AlreadyRunning(override val message: String) : EnvironmentError(409, message)
    data class ResourceExhausted(override val message: String) : EnvironmentError(507, message)
    data class PermissionDenied(override val message: String) : EnvironmentError(403, message)
    data class Unknown(override val message: String) : EnvironmentError(-999, message)
}