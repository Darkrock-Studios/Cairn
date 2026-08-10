import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm("desktop")
    androidLibrary {
        namespace = "com.darkrockstudios.cairn"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
        }

        // Without this, compose resources (fonts/icons) are not packaged
        // into the AAR at all — verified empty in the M10 size audit.
        androidResources {
            enable = true
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "Cairn"
        browser {
            commonWebpackConfig {
                outputFileName = "cairn.js"
            }
        }
        binaries.library()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.compose.ui.backhandler)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.jetbrains.kotlin.test)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.jetbrains.kotlin.test.junit)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.darkrockstudios.cairn.generated.resources"
}

group = "com.darkrockstudios"
version = providers.gradleProperty("library.version").getOrElse("0.0.0-SNAPSHOT")

mavenPublishing {
    coordinates(artifactId = "cairn")
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name.set("Cairn")
        description.set("A Compose Multiplatform About screen for the Dark Rock Studios family of apps.")
        url.set("https://github.com/Darkrock-Studios/Cairn")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/Darkrock-Studios/Cairn/issues")
        }
        scm {
            connection.set("scm:git:git://github.com/Darkrock-Studios/Cairn.git")
            developerConnection.set("scm:git:ssh://github.com/Darkrock-Studios/Cairn.git")
            url.set("https://github.com/Darkrock-Studios/Cairn")
        }
        developers {
            developer {
                name.set("Adam Brown")
                id.set("Wavesonics")
                email.set("adamwbrown@gmail.com")
            }
        }
    }
}
