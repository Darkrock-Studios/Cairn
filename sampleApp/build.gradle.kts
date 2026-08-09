@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm("desktop")
    androidLibrary {
        namespace = "com.darkrockstudios.cairn.sample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
        }
    }
    wasmJs {
        outputModuleName = "sampleApp"
        browser {
            commonWebpackConfig {
                outputFileName = "sampleApp.js"
            }
        }
        binaries.executable()
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-use-new-exception-proposal")
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CairnSample"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.cairn)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.darkrockstudios.cairn.sample.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.darkrockstudios.cairn.sample"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register<Copy>("updateDemo") {
    description = "Builds the WASM distribution and copies it to the docs directory for GitHub Pages"
    group = "distribution"

    dependsOn("wasmJsBrowserDistribution")

    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(layout.projectDirectory.dir("../docs"))

    doLast {
        val docsDir = layout.projectDirectory.dir("../docs").asFile
        val buildDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable").get().asFile

        val newWasmFiles =
            buildDir.listFiles { file -> file.extension == "wasm" }?.map { it.name }?.toSet() ?: emptySet()

        docsDir.listFiles { file -> file.extension == "wasm" }?.forEach { oldFile ->
            if (oldFile.name !in newWasmFiles) {
                println("Removing old WASM file: ${oldFile.name}")
                oldFile.delete()
            }
        }

        println("Demo updated in docs/ directory")
    }
}
