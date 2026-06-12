import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val generateAndroidAppVersion by tasks.registering {
    val versionName = providers.gradleProperty("appVersionName")
    val versionCode = providers.gradleProperty("appVersionCode")
    val outputDir = layout.buildDirectory.dir("generated/kotlin/androidMain")
    inputs.property("versionName", versionName)
    inputs.property("versionCode", versionCode)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/leogallego/ansiblejane/AppVersion.android.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package io.github.leogallego.ansiblejane
            |
            |actual object AppVersion {
            |    actual val name: String = "${versionName.get()}"
            |    actual val code: Int = ${versionCode.get()}
            |}
            """.trimMargin() + "\n"
        )
    }
}

val generateDesktopAppVersion by tasks.registering {
    val versionName = providers.gradleProperty("appVersionName")
    val versionCode = providers.gradleProperty("appVersionCode")
    val outputDir = layout.buildDirectory.dir("generated/kotlin/desktopMain")
    inputs.property("versionName", versionName)
    inputs.property("versionCode", versionCode)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/leogallego/ansiblejane/AppVersion.desktop.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package io.github.leogallego.ansiblejane
            |
            |actual object AppVersion {
            |    actual val name: String = "${versionName.get()}"
            |    actual val code: Int = ${versionCode.get()}
            |}
            """.trimMargin() + "\n"
        )
    }
}

kotlin {
    android {
        namespace = "io.github.leogallego.ansiblejane.composeapp"
        compileSdk = 37
        minSdk = 31
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    // iOS targets added in Phase 8 (requires macOS + Kotlin/Native)
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)

            implementation(libs.cmp.lifecycle.viewmodel)
            implementation(libs.cmp.lifecycle.viewmodel.compose)
            implementation(libs.cmp.lifecycle.runtime.compose)
            implementation(libs.cmp.navigation.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.ktor.client.core)

            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.markdown.renderer.code)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.get().kotlin.srcDir(generateAndroidAppVersion.map { it.outputs.files.singleFile })
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }

        desktopMain.kotlin.srcDir(generateDesktopAppVersion.map { it.outputs.files.singleFile })
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }

        // iosMain.dependencies added in Phase 8
        // iosMain.dependencies {
        //     implementation(libs.ktor.client.darwin)
        // }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.leogallego.ansiblejane.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "AnsibleJane"
            packageVersion = providers.gradleProperty("appVersionName").get().substringBefore("-")

            linux {
                modules("jdk.security.auth")
            }
        }
    }
}
