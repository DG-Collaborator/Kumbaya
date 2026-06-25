import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // same targets as :shared so every platform can depend on the core.
    // No framework {} binary here: core is consumed through :shared, not by an
    // iOS app directly, so it stays a plain multiplatform library for now.
    iosArm64()
    iosSimulatorArm64()
    jvm()

    androidLibrary {
        namespace = "com.app.backendplug_kmp.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            // part of core's PUBLIC api (HttpClient types, the suspend fetch),
            // so modules depending on core get these transitively
            api(libs.ktor.client.core)
            api(libs.kotlinx.coroutines.core)

            // internal plumbing of the JSON source only
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqlite.jdbc)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
