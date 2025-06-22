plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    api(libs.gradle.android)
    api(libs.gradle.kotlin)
    api(libs.gradle.cocoapods)
    api(libs.gradle.detekt)
    api(libs.gradle.dokka)
    api(libs.gradle.kotlin.compose)
    api(libs.gradle.jetbrains.compose)
    api(libs.gradle.kotlin.jvm)
    api(libs.gradle.maven.publish)
}

val autoDetectPluginRegex = Regex("""^(?:public\s+)?class\s+(\w+)BuildLogicPlugin\s*:.*$""", RegexOption.MULTILINE)
val autoDetectedPlugins = file("src").walkBottomUp().filter { it.extension == "kt" }.flatMap { file ->
    autoDetectPluginRegex.findAll(file.readText()).map { it.groupValues[1] }
}.toList()

gradlePlugin {
    plugins {
        autoDetectedPlugins.forEach {  variant ->
            create("com.ensody.build-logic.${variant.lowercase()}") {
                id = name
                implementationClass = "com.ensody.buildlogic.${variant}BuildLogicPlugin"
            }
        }
    }
}
