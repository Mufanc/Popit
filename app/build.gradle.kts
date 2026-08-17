plugins {
    id("com.android.application")
}

val cfgReleaseStoreFile = providers.environmentVariable("RELEASE_STORE_FILE").orNull
val cfgReleaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
val cfgReleaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
val cfgReleaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull

android {
    namespace = "xyz.mufanc.popit"
    compileSdk = 37

    defaultConfig {
        applicationId = "xyz.mufanc.popit"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = cfgReleaseStoreFile?.let { file(it) }
            storePassword = cfgReleaseStorePassword
            keyAlias = cfgReleaseKeyAlias
            keyPassword = cfgReleaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
