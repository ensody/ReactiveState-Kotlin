enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("rootLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name += "-root"

val ignorePaths = mutableSetOf("build", "docs", "gradle", "src")
val rootLibs = file("../gradle/libs.versions.toml").readText().replace(Regex("#.*"), "")
if ("org.jetbrains.kotlin.plugin.compose" !in rootLibs || "org.jetbrains.compose" !in rootLibs) {
    ignorePaths.add("build-logic-compose")
}
fun autoDetectModules(root: File) {
    for (file in root.listFiles()) {
        if (file.name.startsWith(".") || file.name in ignorePaths) {
            continue
        }
        if (file.isDirectory) {
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
