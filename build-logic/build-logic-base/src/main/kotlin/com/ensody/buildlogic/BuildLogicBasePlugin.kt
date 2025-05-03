@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

class BuildLogicBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}

class BuildLogicBaseDependencies(
    val rootProject: Project,
) {
    val libs: VersionCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
}

lateinit var buildLogicBaseDeps: BuildLogicBaseDependencies

fun Project.initBuildLogicBase() {
    require(isRootProject) { "initBuildLogic() must be called on the root project!" }
    buildLogicBaseDeps = BuildLogicBaseDependencies(this)
    version = detectProjectVersion()
    println("Version: $version")
    subprojects {
        version = rootProject.version
    }
    setupBuildLogicBase()
}

fun Project.setupBuildLogicBase() {
    pluginManager.apply("com.ensody.build-logic-base")

    if (isRootProject) return

    group = (listOf(rootProject.group) + project.path.trimStart(':').split(".").dropLast(1))
        .joinToString(".")
}

val libs get() = buildLogicBaseDeps.libs
