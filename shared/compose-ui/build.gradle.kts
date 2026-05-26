plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    androidLibrary {
        namespace = "com.sdk.translation.compose"
        compileSdk = 35
        minSdk = 24
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComposeUI"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
        }
    }
}
