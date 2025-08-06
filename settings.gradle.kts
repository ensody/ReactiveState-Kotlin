import kotlin.collections.contains

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

val ignorePaths = setOf("build", "docs", "gradle", "src")
fun autoDetectModules(root: File) {
    for (file in root.listFiles()) {
        if (file.name.startsWith(".") || file.name in ignorePaths) {
            continue
        }
        if (file.isDirectory()) {
            val children = file.list()
            if ("settings.gradle.kts" in children) continue
            if (children.any { it == "build.gradle.kts" }) {
                include(":" + file.relativeTo(rootDir).path.replace("/", ":").replace("\\", ":"))
            } else {
                autoDetectModules(file)
            }
        }
    }
}

autoDetectModules(rootDir)
