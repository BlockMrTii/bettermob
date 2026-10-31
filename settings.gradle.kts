pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

rootProject.name = "bettermob-parent"

include("bettermob-api", "bettermob-core", "bettermob-paper", "bettermob-fabric")
