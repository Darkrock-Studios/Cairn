import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
    }
}

android {
    namespace = "com.darkrockstudios.cairn.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.darkrockstudios.cairn.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
    }
}

dependencies {
    implementation(projects.sampleApp)

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)

    implementation(libs.activity.compose)
    implementation(libs.activity.ktx)
}
