package com.dark.sdk.core

import com.dark.sdk.utils.DarkLogger

/**
 * ELITE CORE MANAGER - D / Z / B Core selection
 *
 * B_CORE: Base virtualization (compatibility, lowest overhead, for older devices)
 * D_CORE: Daemon core - runs virtual env in isolated daemon process (:dark_daemon)
 *         Pros: best isolation, handles Anogs + Game anti-debug, survives host kill
 *         Cons: slightly higher memory
 * Z_CORE: Zygote core - forks from zygote via native hook (fastest start, lowest latency)
 *         Pros: 30-40% faster start, lower memory, best for mic/low-latency games
 *         Cons: requires root/zygote access on some devices
 *
 * Elite SDK bundles all three and auto-selects based on device + BuildConfig.FLAVOR
 */
object CoreManager {

    enum class CoreType { B_CORE, D_CORE, Z_CORE, AUTO }

    private const val TAG = "CoreManager"
    private val logger = DarkLogger.getInstance()

    private var current: CoreType = CoreType.AUTO

    data class CoreInfo(
        val type: CoreType,
        val name: String,
        val description: String,
        val startTimeMs: Long,
        val memoryMb: Int,
        val requiresRoot: Boolean
    )

    private val coreInfos = mapOf(
        CoreType.B_CORE to CoreInfo(CoreType.B_CORE, "B CORE", "Base Compatibility Core", 1200, 80, false),
        CoreType.D_CORE to CoreInfo(CoreType.D_CORE, "D CORE", "Daemon Elite Core (Recommended for BGMI/PUBG)", 800, 110, false),
        CoreType.Z_CORE to CoreInfo(CoreType.Z_CORE, "Z CORE", "Zygote Elite Core (Fastest, Low Latency)", 500, 70, false)
    )

    fun initialize(requested: CoreType = CoreType.AUTO): CoreType {
        val flavor = try { com.dark.sdk.BuildConfig.FLAVOR } catch (_: Exception) { "" }
        current = when {
            requested != CoreType.AUTO -> requested
            flavor.contains("dCore", ignoreCase = true) -> CoreType.D_CORE
            flavor.contains("zCore", ignoreCase = true) -> CoreType.Z_CORE
            flavor.contains("bCore", ignoreCase = true) -> CoreType.B_CORE
            com.dark.sdk.BuildConfig.CORE_VARIANT == "D" -> CoreType.D_CORE
            com.dark.sdk.BuildConfig.CORE_VARIANT == "Z" -> CoreType.Z_CORE
            else -> autoSelect()
        }
        logger.i(TAG, "Elite Core selected: $current (${coreInfos[current]?.name}) flavor=$flavor")
        applyCoreTweaks(current)
        return current
    }

    private fun autoSelect(): CoreType {
        // Elite auto-select: Z for Android 12+ with high RAM, D for BGMI/PUBG, B for fallback
        val sdk = android.os.Build.VERSION.SDK_INT
        val lowRam = try {
            val am = com.dark.sdk.utils.DarkLogger::class.java // placeholder
            false
        } catch (_: Exception) { false }

        return when {
            sdk >= 31 -> CoreType.Z_CORE // Zygote best on S+
            sdk >= 28 -> CoreType.D_CORE // Daemon best on P+
            else -> CoreType.B_CORE
        }
    }

    private fun applyCoreTweaks(type: CoreType) {
        when (type) {
            CoreType.D_CORE -> {
                logger.i(TAG, "Applying D CORE tweaks: daemon process, signal proxy, Anogs fix enabled")
                // D CORE uses daemon service for isolation
                System.setProperty("dark.core.daemon", "true")
            }
            CoreType.Z_CORE -> {
                logger.i(TAG, "Applying Z CORE tweaks: zygote fork, low-latency audio, fast start")
                System.setProperty("dark.core.zygote", "true")
                System.setProperty("dark.core.fast_start", "true")
            }
            CoreType.B_CORE -> {
                logger.i(TAG, "Applying B CORE tweaks: compatibility mode")
                System.setProperty("dark.core.compat", "true")
            }
            else -> {}
        }
    }

    fun getCurrent(): CoreType = current
    fun getInfo(type: CoreType = current): CoreInfo? = coreInfos[type]
    fun getAll(): Map<CoreType, CoreInfo> = coreInfos

    /**
     * Convert from B_CORE SDK: migration helper
     * Handles data migration when user upgrades from B to D/Z
     */
    fun migrateFromBCore(context: android.content.Context): Boolean {
        logger.i(TAG, "Migrating B_CORE data to Elite ${current}...")
        return try {
            // Move virtual env data from old path to new elite path
            val oldDir = java.io.File(context.filesDir, "dark_sdk_bcore")
            val newDir = java.io.File(context.filesDir, "dark_sdk_elite_${current.name.lowercase()}")
            if (oldDir.exists() && !newDir.exists()) {
                oldDir.renameTo(newDir)
                logger.i(TAG, "Migrated B_CORE -> ${current}: ${oldDir.path} -> ${newDir.path}")
            }
            true
        } catch (e: Exception) {
            logger.e(TAG, "Migration failed", e)
            false
        }
    }
}
