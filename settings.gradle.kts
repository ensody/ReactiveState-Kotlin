pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        if (System.getenv("RUNNING_ON_CI") != "true") {
            mavenLocal()
        }
    }
}

rootProject.name = "ReactiveState"

include(":reactivestate-bom")

include(":reactivestate-android")
include(":reactivestate-android-test")
include(":reactivestate-compose")
include(":reactivestate-core")
include(":reactivestate-core-test")
