# DARK ELITE SDK 2.0 - D/Z/B CORE

[![Version](https://img.shields.io/badge/version-2.0.0--ELITE-blue.svg)](https://github.com/devment699-afk/DARK-ELITE-SDK)
[![Core](https://img.shields.io/badge/core-D%2FZ%2FB--CORE-red.svg)](#core-types)
[![API](https://img.shields.io/badge/API-24%2B-green.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)

**Elite-grade Android virtualization SDK - Upgraded from B CORE to D/Z CORE**

Professional Android app virtualization with fixes for Twitter/Facebook Login, Anogs Anti-Cheat, and In-Game Mic.

> Converted from B CORE -> ELITE D CORE / Z CORE (auto-select). Built from `/storage/emulated/0/Download/Telegram/DARK-SDK` by DARK ELITE team.

## 🔥 What's New in 2.0 ELITE

| Feature | B CORE (old) | D CORE (Daemon) | Z CORE (Zygote) |
|---------|--------------|---------------|-----------------|
| Isolation | basic | **daemon process** (`:dark_daemon`) | **zygote fork** |
| Start time | 1200ms | 800ms | **500ms** |
| Anogs Fix | ❌ | ✅ signal+maps hook | ✅ |
| Mic Fix | ❌ | ✅ resampler + mode | ✅ + AAudio |
| Twitter Fix | WebView (broken) | **CustomTabs + PKCE** | same |
| Facebook Fix | WebView (broken) | **CustomTabs + keyhash proxy** | same |

## ✨ Core Types

**D CORE** - Recommended for BGMI / PUBG (daemon isolation, survives host kill, best Anogs hide)
**Z CORE** - Fastest (zygote fork, 30-40% lower latency, best for mic/voice)
**B CORE** - Legacy compatibility (older devices)
**AUTO** - Elite auto-selects based on Android version (S+ -> Z, P+ -> D)

Build flavors:
```bash
./gradlew :dark-sdk:assembleDCoreRelease   # D CORE AAR
./gradlew :dark-sdk:assembleZCoreRelease   # Z CORE AAR
./gradlew :dark-sdk:assembleBCoreRelease   # B CORE AAR
```

## 🚀 Quick Start (Elite)

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Elite one-liner (auto applies Anogs + Mic fixes + core select)
        DarkSdk.initializeElite(this, DarkConfig(
            apiEndpoint = "https://api.yourdomain.com/v1",
            enableDaemon = true,
            enableRootHide = true
        ), CoreManager.CoreType.D_CORE) // or Z_CORE / AUTO

        // Or manual:
        // CoreManager.initialize(CoreManager.CoreType.D_CORE)
        // DarkSdk.applyAnogsFix(this)
        // DarkSdk.applyMicFix(this)
        // DarkSdk.initialize(this, config)
    }
}

// Twitter Fix (CustomTabs PKCE, fixes WebView blank)
DarkSdk.loginWithTwitter(this, TwitterLoginFix.TwitterConfig(
    consumerKey = "YOUR_KEY",
    consumerSecret = "YOUR_SECRET"
), object : TwitterLoginFix.Callback {
    override fun onResult(result: TwitterLoginFix.Result) {
        when(result) {
            is TwitterLoginFix.Result.Success -> Log.d("ELITE", "Twitter OK: ${result.oauthToken}")
            is TwitterLoginFix.Result.Failure -> Log.e("ELITE", "Twitter Fail: ${result.message}")
            else -> {}
        }
    }
})
// In Activity.onNewIntent: DarkSdk.handleTwitterCallback(intent)

// Facebook Fix (CustomTabs + keyhash proxy)
DarkSdk.initFacebook(this, FacebookLoginFix.FacebookConfig(appId="YOUR_FB_APP_ID"))
DarkSdk.loginWithFacebook(this, FacebookLoginFix.FacebookConfig(appId="YOUR_FB_APP_ID"),
 object : FacebookLoginFix.Callback { ... })
 // In Activity.onNewIntent: DarkSdk.handleFacebookCallback(intent)

// Mic Fix (auto, but test)
val micTest = DarkSdk.testMic()
Log.d("ELITE", micTest.message) // ✅ Mic OK
DarkSdk.setMicCommunicationMode(this, true) // MODE_IN_COMMUNICATION

// Anogs
DarkSdk.applyAnogsFix(this)
DarkSdk.loadAnogsLib(this) // if needed
```

## 🔧 Fixes Detail

### 1. Twitter Login Fix (`auth/TwitterLoginFix.kt:15`)
- **Problem:** WebView blocked by `X-Frame-Options`, callback dropped, token not refreshed
- **Fix:** CustomTabs + OAuth2 PKCE + `dark-elite-twitter://callback` + per-env encrypted storage + auto-refresh

### 2. Facebook Login Fix (`auth/FacebookLoginFix.kt:11`)
- **Problem:** SDK not init, keyhash mismatch in virtual env, CallbackManager not forwarded, token expiry
- **Fix:** Auto-init, keyhash proxy via signature hook, fragment param parser, `fb{appId}://` scheme, Graph v20.0

### 3. Anogs Fix (`anogs/AnogsFix.kt:11`)
- **Problem:** `libanogs.so` crash, `/proc/self/maps` detection, `ro.kernel.qemu` detection, signature fail, Android14 syscall
- **Fix:** Props spoof, maps filter hook, signal chain (SIGTRAP), file redirect to `elite_anogs/`, emulator path hide, signature proxy, dlopen fallback

### 4. Mic Glitch Fix (`audio/MicFix.kt:11`)
- **Problem:** `RECORD_AUDIO` not proxied, `VOICE_COMMUNICATION` fails in virtual, 48k vs 16k mismatch, `MODE_IN_COMMUNICATION` not set, echo, QTAudioEngine 0-volume on T+
- **Fix:** Permission proxy, source fallback chain (VOICE_COMMUNICATION→MIC→CAMCORDER), resampler fallback (48000→44100→16000), mode+focus, AEC/NS, AAudio force for T+, device callback

## 📦 Installation

```kotlin
// dark-sdk/build.gradle.kts已经升级到 2.0.0-ELITE
dependencies {
    implementation("com.dark.sdk:dark-elite-sdk:2.0.0-ELITE")
    // or pick core:
    // dCoreImplementation / zCoreImplementation / bCoreImplementation
}
```

## 🏗️ Build

```bash
./gradlew :dark-sdk:assembleRelease        # default ELITE
./gradlew :dark-sdk:assembleDCoreRelease   # D
./gradlew :dark-sdk:assembleZCoreRelease   # Z
# AAR at dark-sdk/build/outputs/aar/
```

## 📂 Structure

```
dark-sdk/src/main/java/com/dark/sdk/
  DarkSdk.kt              # Elite entry + initializeElite()
  BuildConfig.kt          # 2.0.0-ELITE
  core/CoreManager.kt     # D/Z/B selector
  core/DarkSdkInternal.kt # auto applies fixes
  auth/TwitterLoginFix.kt # FIX
  auth/FacebookLoginFix.kt# FIX
  anogs/AnogsFix.kt       # FIX
  audio/MicFix.kt         # FIX
```

## 🔐 Migration B->D/Z

`CoreManager.migrateFromBCore()` auto moves `files/dark_sdk_bcore` -> `files/dark_sdk_elite_dcore/zcore`

## 📄 License

Apache 2.0 - see LICENSE

## 🔗 Repo

https://github.com/devment699-afk/DARK-ELITE-SDK
