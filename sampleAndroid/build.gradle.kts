plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.sdk.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sdk.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:compose-ui"))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
}
