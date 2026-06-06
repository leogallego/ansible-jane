import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "io.github.leogallego.ansiblejane.shared"
        compileSdk = 36
        minSdk = 31
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTest {}
    }

    jvm()

    // iOS targets added in Phase 8 (requires macOS + Kotlin/Native)
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.androidx.datastore.preferences)

            implementation(libs.koin.core)

            implementation(libs.koog.openai.client)
            implementation(libs.koog.google.client)
            implementation(libs.koog.http.ktor)
            implementation(libs.mcp.sdk.client)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.tink.android)
            implementation(libs.androidx.work.runtime)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        // iosMain.dependencies added in Phase 8
        // iosMain.dependencies {
        //     implementation(libs.ktor.client.darwin)
        // }
    }
}
