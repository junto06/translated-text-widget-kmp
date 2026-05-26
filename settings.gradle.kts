rootProject.name = "translated-text-widget"
include(":shared:core")
include(":shared:compose-ui")
include(":sampleAndroid")

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
