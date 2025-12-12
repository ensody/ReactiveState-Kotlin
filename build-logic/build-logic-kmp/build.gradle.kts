plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    api(project(":build-logic-utils"))
    api(rootLibs.gradle.cocoapods)
    api(rootLibs.gradle.detekt)
    api(rootLibs.gradle.dokka)
    api(rootLibs.gradle.kotlin.jvm)
    api(rootLibs.gradle.maven.publish)
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
