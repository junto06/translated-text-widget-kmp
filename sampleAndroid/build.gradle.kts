import java.util.Properties

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

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())
        val deepseekApiKey = localProps.getProperty("deepseek.api.key")
            ?: System.getenv("DEEPSEEK_API_KEY")
            ?: "" // hardcode key here for local testing on default/feature branches or use local.properties file
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:compose-ui"))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
}
