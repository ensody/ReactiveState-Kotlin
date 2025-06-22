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

val ignorePaths = setOf("build", "build-logic", "gradle", "src")
fun autoDetectModules(root: File) {
    for (file in root.listFiles()) {
        if (file.name.startsWith(".") || file.name in ignorePaths) {
            continue
        }
        if (file.isDirectory()) {
            if (file.list().any { it == "build.gradle.kts" }) {
                include(":" + file.relativeTo(rootDir).path.replace("/", ":").replace("\\", ":"))
            } else {
                autoDetectModules(file)
            }
        }
    }
}

autoDetectModules(rootDir)
