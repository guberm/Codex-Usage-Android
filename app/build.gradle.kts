plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.guberdev.codexusage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.guberdev.codexusage"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.1.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            val keystorePath = providers.environmentVariable("CODEX_USAGE_KEYSTORE_PATH").orNull
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(keystorePath)
                    storePassword = providers.environmentVariable("CODEX_USAGE_KEYSTORE_PASSWORD").get()
                    keyAlias = providers.environmentVariable("CODEX_USAGE_KEY_ALIAS").get()
                    keyPassword = providers.environmentVariable("CODEX_USAGE_KEY_PASSWORD").get()
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260719")
}
