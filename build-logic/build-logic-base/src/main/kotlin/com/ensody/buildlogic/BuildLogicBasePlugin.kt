@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import com.android.build.gradle.internal.cxx.io.writeTextIfDifferent
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

fun Project.initBuildLogicBase(block: Project.() -> Unit) {
    require(isRootProject) { "initBuildLogic() must be called on the root project!" }
    buildLogicBaseDeps = BuildLogicBaseDependencies(this)
    version = detectProjectVersion()
    println("Version: $version")
    subprojects {
        version = rootProject.version
    }

    // Setup detekt.yml
    val rules = BuildLogicBasePlugin::class.java.module.getResourceAsStream("detekt.yml").reader().readText()
    file("build/build-logic/detekt.yml").writeTextIfDifferent(rules)

    block()
}

fun Project.setupBuildLogicBase(block: Project.() -> Unit) {
    group = (listOf(rootProject.group) + project.path.trimStart(':').split(".").dropLast(1))
        .joinToString(".")
    block()
    afterEvaluate {
        val generatedRoot = getGeneratedBuildFilesRoot()
        val generatedRelativePaths = generatedFiles[this.path].orEmpty().flatMap { it.relativeTo(generatedRoot).withParents().map { it.toString() } }.toSet()
        for (file in generatedRoot.walkBottomUp()) {
            if (file == generatedRoot) continue
            if (file.relativeTo(generatedRoot).toString() !in generatedRelativePaths) {
                file.deleteRecursively()
            }
        }
    }
}

val libs get() = buildLogicBaseDeps.libs
