import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.parcelize")
    kotlin("plugin.serialization") version "2.3.20"
}

android {
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.14206865"

    lint {
        checkReleaseBuilds = false
        disable += listOf("ChromeOsAbiSupport", "MissingApplicationIcon")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xreport-perf",
                "-Xexplicit-api=strict",
            )
        }
    }

    defaultConfig {
        applicationId = "app.vitriol"
        minSdk = 24
        targetSdk = 36
        versionCode = 1000
        versionName = "v1.0"

        androidResources { localeFilters += setOf("en") }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            // include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as BaseVariantOutputImpl
            val abiName = output.filters.find { it.filterType == "ABI" }?.identifier

            if (abiName != null) {
                val baseVersionCode = variant.versionCode
                val abiVersionCode =
                    when (abiName) {
                        "x86" -> baseVersionCode - 3
                        "x86_64" -> baseVersionCode - 2
                        "armeabi-v7a" -> baseVersionCode - 1
                        "arm64-v8a" -> baseVersionCode
                        else -> baseVersionCode
                    }

                (output as ApkVariantOutputImpl).versionCodeOverride = abiVersionCode
                output.outputFileName = ("vitriol-${variant.versionName}-$abiName.apk")
            }
        }
    }

    val userHomeProps =
        Properties().apply {
            val userGradleFile = rootProject.file(".credentials.properties")
            if (userGradleFile.exists()) {
                load(userGradleFile.inputStream())
            }
        }

    signingConfigs {
        create("release") {
            storeFile = file(userHomeProps.getProperty("KEYSTORE_PATH"))
            storePassword = userHomeProps.getProperty("STORE_PASSWORD")
            keyAlias = userHomeProps.getProperty("KEY_ALIAS")
            keyPassword = userHomeProps.getProperty("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isShrinkResources = true
        }
        getByName("debug") {
            isShrinkResources = true
            isDebuggable = false
            isMinifyEnabled = true
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.3.20"
    }

    namespace = "app.vitriol"

    dependenciesInfo {
        includeInApk = true
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotlin.reflect)
    implementation(libs.exp4j)
}
