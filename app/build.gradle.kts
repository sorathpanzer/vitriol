import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin)
    alias(libs.plugins.detekt.kotlin)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
}

android {
    compileSdk = 36
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

        androidResources {
            localeFilters += setOf("en")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    val userHomeProps = Properties().apply {
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
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isShrinkResources = false
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    namespace = "app.vitriol"

    dependenciesInfo {
        includeInApk = false
    }
}

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abi = output.filters
                    .find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                    ?.identifier
    
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    val abiSuffix = abi?.let { "-$it" } ?: ""
                    output.outputFileName.set("vitriol-${android.defaultConfig.versionName}$abiSuffix.apk")
                }
            }
        }
    }

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.biometric)
}
