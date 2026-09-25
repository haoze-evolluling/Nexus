import java.io.File
import java.util.Properties
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = listOf(
    rootProject.file("Nexus-keystore/keystore.properties"),
    rootProject.file("SyncTouch-keystore/keystore.properties")
).firstOrNull { it.exists() } ?: rootProject.file("Nexus-keystore/keystore.properties")

val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStoreFilePath: String? = keystoreProperties.getProperty("storeFile")
val releaseStorePassword: String? = keystoreProperties.getProperty("storePassword")
val releaseKeyAlias: String? = keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword: String? = keystoreProperties.getProperty("keyPassword")

val releaseKeystoreFile: File? = releaseStoreFilePath?.let { rootProject.file(it) }

val signDebugWithRelease = project.findProperty("signDebugWithRelease") in listOf("true", "1", "")

android {
    namespace = "com.haoze.nexus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.haoze.nexus"
        minSdk = 28
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystoreFile != null && releaseKeystoreFile.exists() &&
            !releaseStorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank()
        ) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword ?: releaseStorePassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            if (signDebugWithRelease) {
                signingConfigs.findByName("release")?.let { releaseSigningConfig ->
                    signingConfig = releaseSigningConfig
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { releaseSigningConfig ->
                signingConfig = releaseSigningConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }
    ndkVersion = "27.0.12077973"
}

val apkVersionName = android.defaultConfig.versionName ?: "unknown"

listOf("debug", "release").forEach { buildType ->
    val capitalizedBuildType = buildType.replaceFirstChar { it.uppercase() }
    val apkOutputDirectory = layout.buildDirectory.dir("outputs/apk/$buildType")
    val versionedApkOutputDirectory = layout.buildDirectory.dir("outputs/apk/versioned/$buildType")

    val copyApkTask = tasks.register<Copy>("copy${capitalizedBuildType}ApkWithVersion") {
        dependsOn("assemble$capitalizedBuildType")
        from(apkOutputDirectory)
        include("app-$buildType.apk")
        rename("app-$buildType.apk", "Nexus-$buildType-v$apkVersionName.apk")
        into(versionedApkOutputDirectory)
    }

    tasks.configureEach {
        if (name == "assemble$capitalizedBuildType") {
            finalizedBy(copyApkTask)
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
