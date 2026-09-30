import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val signingProps = Properties().apply {
    val f = file(System.getProperty("user.home") + "/nexun-signing.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

fun cred(env: String, key: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: signingProps.getProperty(key)?.takeIf { it.isNotBlank() }

val keystorePath =
    System.getenv("NEXUN_KEYSTORE")?.takeIf { it.isNotBlank() }
        ?: "${System.getProperty("user.home")}/nexun.jks"
val releaseKeystore = file(keystorePath)
val canSignRelease = releaseKeystore.exists() &&
    cred("NEXUN_STORE_PASSWORD", "storePassword") != null

android {
    namespace = "nx.screen.ds"
    compileSdk = 36

    defaultConfig {
        applicationId = "nx.screen.ds"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        if (canSignRelease) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = cred("NEXUN_STORE_PASSWORD", "storePassword")
                keyAlias = cred("NEXUN_KEY_ALIAS", "keyAlias") ?: "nexun"
                keyPassword = cred("NEXUN_KEY_PASSWORD", "keyPassword")
                    ?: cred("NEXUN_STORE_PASSWORD", "storePassword")
                // JKS para el keystore propio; PKCS12 para el testkey de AOSP.
                cred("NEXUN_STORE_TYPE", "storeType")?.let { storeType = it }
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.dynamiccolor)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    debugImplementation(libs.androidx.ui.tooling)
}
