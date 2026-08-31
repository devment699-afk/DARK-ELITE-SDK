plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.dark.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 200
        versionName = "2.0.0-ELITE"
        consumerProguardFiles("proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Elite Core: expose build flags for D/Z/B selection
        buildConfigField("String", "CORE_TYPE", "\"ELITE\"")
        buildConfigField("String", "CORE_VARIANT", "\"D-Z-B\"")
        buildConfigField("boolean", "ELITE_MIC_FIX", "true")
        buildConfigField("boolean", "ELITE_ANOGS_FIX", "true")
        buildConfigField("boolean", "ELITE_AUTH_FIX", "true")
    }

    flavorDimensions += "core"
    productFlavors {
        create("bCore") {
            dimension = "core"
            applicationIdSuffix = ".bcore"
            versionNameSuffix = "-B-CORE"
            buildConfigField("String", "CORE_TYPE", "\"B_CORE\"")
            buildConfigField("String", "CORE_VARIANT", "\"B\"")
        }
        create("dCore") {
            dimension = "core"
            // D CORE = Daemon-driven Elite core (recommended for BGMI/PUBG)
            applicationIdSuffix = ".dcore"
            versionNameSuffix = "-D-CORE"
            buildConfigField("String", "CORE_TYPE", "\"D_CORE\"")
            buildConfigField("String", "CORE_VARIANT", "\"D\"")
            buildConfigField("boolean", "USE_DAEMON_CORE", "true")
        }
        create("zCore") {
            dimension = "core"
            // Z CORE = Zygote-injected Elite core (fast fork, low latency)
            applicationIdSuffix = ".zcore"
            versionNameSuffix = "-Z-CORE"
            buildConfigField("String", "CORE_TYPE", "\"Z_CORE\"")
            buildConfigField("String", "CORE_VARIANT", "\"Z\"")
            buildConfigField("boolean", "USE_ZYGOTE_CORE", "true")
        }
    }

    buildTypes {
        release {
            minifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            minifyEnabled = false
            debuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Annotation processing
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    implementation("com.squareup:javapoet:1.13.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // ELITE FIX: Auth - Custom Tabs for Twitter/Facebook OAuth (fixes WebView blank/callback issues)
    implementation("androidx.browser:browser:1.7.0")
    
    // ELITE FIX: Mic - Audio handling
    implementation("androidx.media:media:1.6.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "1.8"
    }
}

// Maven publishing configuration
group = "com.dark.sdk"
version = "2.0.0-ELITE"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.dark.sdk"
            artifactId = "dark-elite-sdk"
            version = "2.0.0-ELITE"
            from(components["release"])
            
            pom {
                name.set("DARK ELITE SDK 2.0 - D/Z/B CORE Professional Virtualization")
                description.set("Elite-grade Android virtualization SDK - D_CORE (Daemon) / Z_CORE (Zygote) / B_CORE (Base) - Fixed Twitter/FB Login + Anogs + Mic")
                url.set("https://github.com/dark-sdk/dark-sdk")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("dark-team")
                        name.set("DARK SDK Team")
                        email.set("support@dark-sdk.dev")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/dark-sdk/dark-sdk.git")
                    developerConnection.set("scm:git:https://github.com/dark-sdk/dark-sdk.git")
                    url.set("https://github.com/dark-sdk/dark-sdk")
                }
            }
        }
    }
}