package com.dark.sdk.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dark.sdk.DarkSdk
import com.dark.sdk.api.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    
    private var currentEnvironment: EnvironmentHandle? = null
    private val sampleUserKey = "DEMO-KEY-12345"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
        checkLicenseStatus()
    }
    
    private fun setupUI() {
        // License activation
        findViewById<View>(R.id.btnActivate).setOnClickListener {
            activateLicense()
        }
        
        // Create environment
        findViewById<View>(R.id.btnCreateEnv).setOnClickListener {
            createEnvironment()
        }
        
        // Start environment
        findViewById<View>(R.id.btnStartEnv).setOnClickListener {
            startEnvironment()
        }
        
        // Stop environment
        findViewById<View>(R.id.btnStopEnv).setOnClickListener {
            stopEnvironment()
        }
        
        // Execute command
        findViewById<View>(R.id.btnExecute).setOnClickListener {
            executeCommand()
        }
        
        // Install APK
        findViewById<View>(R.id.btnInstall).setOnClickListener {
            installApk()
        }
        
        // Get stats
        findViewById<View>(R.id.btnStats).setOnClickListener {
            getStats()
        }
        
        // Check status
        findViewById<View>(R.id.btnCheckStatus).setOnClickListener {
            checkLicenseStatus()
        }
        
        // Set log level
        findViewById<View>(R.id.btnDebugLogs).setOnClickListener {
            DarkSdk.setLogLevel(LogLevel.DEBUG)
            Toast.makeText(this, "Debug logging enabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun activateLicense() {
        val btn = findViewById<View>(R.id.btnActivate)
        btn.isEnabled = false
        
        DarkSdk.activateLicense(sampleUserKey, object : LicenseCallback {
            override fun onSuccess(status: LicenseStatus) {
                runOnUiThread {
                    btn.isEnabled = true
                    updateStatusText("✅ Activated: ${status.expiryDate ?: "Lifetime"} (${status.daysRemaining} days)")
                    updateFeatureFlags(status.features)
                    Toast.makeText(this@MainActivity, "License activated!", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(error: LicenseError) {
                runOnUiThread {
                    btn.isEnabled = true
                    updateStatusText("❌ Failed: ${error.message} (code: ${error.code})")
                    Toast.makeText(this@MainActivity, "Activation failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    
    private fun createEnvironment() {
        val config = EnvironmentConfig(
            packageName = "com.example.targetapp",
            displayName = "Target App Demo",
            enableRoot = false,
            enableXposed = false,
            resourceLimits = ResourceLimits(
                maxMemoryMb = 512,
                maxCpuPercent = 80,
                maxStorageMb = 1024,
                networkAllowed = true
            )
        )
        
        currentEnvironment = DarkSdk.createEnvironment(config)
        currentEnvironment?.let {
            updateStatusText("📦 Environment created: ${it.id}")
            Toast.makeText(this, "Environment created", Toast.LENGTH_SHORT).show()
        } ?: run {
            updateStatusText("❌ Failed to create environment")
        }
    }
    
    private fun startEnvironment() {
        currentEnvironment?.start(object : EnvironmentCallback {
            override fun onSuccess(handle: EnvironmentHandle) {
                runOnUiThread {
                    updateStatusText("▶️ Started: ${handle.id} (PID: ${handle.pid})")
                    Toast.makeText(this@MainActivity, "Environment started", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(error: EnvironmentError) {
                runOnUiThread {
                    updateStatusText("❌ Start failed: ${error.message}")
                    Toast.makeText(this@MainActivity, "Start failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    
    private fun stopEnvironment() {
        currentEnvironment?.stop(object : EnvironmentCallback {
            override fun onSuccess(handle: EnvironmentHandle) {
                runOnUiThread {
                    updateStatusText("⏹️ Stopped: ${handle.id}")
                    Toast.makeText(this@MainActivity, "Environment stopped", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(error: EnvironmentError) {
                runOnUiThread {
                    updateStatusText("❌ Stop failed: ${error.message}")
                    Toast.makeText(this@MainActivity, "Stop failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    
    private fun executeCommand() {
        currentEnvironment?.execute("pm list packages") { result ->
            runOnUiThread {
                when (result) {
                    is ExecutionCallback.Result.Success -> {
                        updateStatusText("📋 Command output:\n${result.output}")
                    }
                    is ExecutionCallback.Result.Failure -> {
                        updateStatusText("❌ Command failed: ${result.error.message}")
                    }
                }
            }
        }
    }
    
    private fun installApk() {
        // Demo - in real app, use file picker
        currentEnvironment?.installApk("/sdcard/Download/sample.apk") { result ->
            runOnUiThread {
                when (result) {
                    is InstallationCallback.Result.Success -> {
                        updateStatusText("📦 Installed: ${result.packageName}")
                        Toast.makeText(this@MainActivity, "APK installed", Toast.LENGTH_SHORT).show()
                    }
                    is InstallationCallback.Result.Failure -> {
                        updateStatusText("❌ Install failed: ${result.error.message}")
                        Toast.makeText(this@MainActivity, "Install failed: ${result.error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun getStats() {
        currentEnvironment?.let { env ->
            val stats = env.getStats()
            updateStatusText("""
                📊 Environment Stats:
                Memory: ${stats.memoryUsageMb} MB
                CPU: ${String.format("%.1f", stats.cpuUsagePercent)}%
                Storage: ${stats.storageUsageMb} MB
                Network RX: ${stats.networkRxBytes / 1024} KB
                Network TX: ${stats.networkTxBytes / 1024} KB
                Uptime: ${stats.uptimeMillis / 1000}s
            """.trimIndent())
        } ?: run {
            updateStatusText("❌ No active environment")
        }
    }
    
    private fun checkLicenseStatus() {
        lifecycleScope.launch {
            val status = DarkSdk.getLicenseStatus()
            runOnUiThread {
                if (status != null) {
                    val state = if (status.isActivated) "✅ ACTIVE" else "❌ INACTIVE"
                    updateStatusText("$state\nServer: ${status.serverStatus}\nExpiry: ${status.expiryDate ?: "None"}\nDays left: ${status.daysRemaining}")
                    updateFeatureFlags(status.features)
                } else {
                    updateStatusText("❓ License status unavailable")
                }
            }
        }
    }
    
    private fun updateStatusText(text: String) {
        runOnUiThread {
            findViewById<View>(R.id.tvStatus).let { tv ->
                (tv as android.widget.TextView).text = text
            }
            Log.d("DarkSampleApp", text)
        }
    }
    
    private fun updateFeatureFlags(features: LicenseFeatures) {
        runOnUiThread {
            findViewById<View>(R.id.tvFeatures).let { tv ->
                (tv as android.widget.TextView).text = """
                    Features:
                    Daemon: ${if (features.daemonEnabled) "✅" else "❌"}
                    Root Hide: ${if (features.rootHideEnabled) "✅" else "❌"}
                    Xposed Hide: ${if (features.xposedHideEnabled) "✅" else "❌"}
                    Custom 1: ${if (features.customFeature1) "✅" else "❌"}
                    Custom 2: ${if (features.customFeature2) "✅" else "❌"}
                """.trimIndent()
            }
        }
    }
}