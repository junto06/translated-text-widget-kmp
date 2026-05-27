rootProject.name = "translated-text-widget"
include(":shared:core")
include(":shared:compose-ui")
include(":sampleAndroid")
include(":sampleIosCompose")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
