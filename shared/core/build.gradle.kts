plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
}

val generatedSdkInfoDir = layout.buildDirectory.dir("generated/sdkInfo/commonMain/kotlin")
val generateSdkInfo by tasks.registering {
    val outputDir = generatedSdkInfoDir
    val version = providers.gradleProperty("sdkVersion").orElse("0.1.0")

    inputs.property("sdkVersion", version)
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("com/sdk/translation/SdkInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.sdk.translation

            internal object SdkInfo {
                const val VERSION: String = "${version.get()}"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidLibrary {
        namespace = "com.sdk.translation"
        compileSdk = 35
        minSdk = 24
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "TranslationSDK"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedSdkInfoDir)
        }
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn(generateSdkInfo)
}
