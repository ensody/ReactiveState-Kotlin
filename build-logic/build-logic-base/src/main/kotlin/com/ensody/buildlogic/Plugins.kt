@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import com.android.build.gradle.internal.cxx.io.writeTextIfDifferent
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// NOTE: The following plugins get registered based on their class name prefix as com.ensody.build-logic.<prefix>

/** BOM setup. */
class BomBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("java-platform")
            }
        }
    }
}

/** Version catalog setup. */
class VersionCatalogBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("version-catalog")
            }
        }
    }
}

/** Maven publication setup. */
class PublishBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("maven-publish")
                pluginManager.apply("com.vanniktech.maven.publish")
            }
            pluginManager.apply("com.ensody.build-logic.dokka")
        }
    }
}

/** Dokka setup. */
class DokkaBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            pluginManager.apply("org.jetbrains.dokka")
        }
    }
}

/** Shared Kotlin setup. */
class KotlinBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("io.gitlab.arturbosch.detekt")
            }
        }
    }
}

/** KMP setup. */
class KmpBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("com.android.library")
                pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            }
            pluginManager.apply("com.ensody.build-logic.kotlin")
        }
    }
}

/** Cocoapods/XCFramework setup. */
class CocoapodsBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            pluginManager.apply("com.ensody.build-logic.kmp")
            if (!isRootProject) {
                pluginManager.apply("org.jetbrains.kotlin.native.cocoapods")
            }
        }
    }
}

/** Jetpack Compose setup. */
class ComposeBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("org.jetbrains.compose")
                pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            }
        }
    }
}

/** JVM setup. */
class JvmBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("org.jetbrains.kotlin.jvm")
            }
            pluginManager.apply("com.ensody.build-logic.kotlin")
        }
    }
}

/** Gradle plugin setup. */
class GradleBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            pluginManager.apply("com.ensody.build-logic.jvm")
            if (!isRootProject) {
                pluginManager.apply("java-gradle-plugin")
            }
        }
    }
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
    val rules = BuildLogicBaseDependencies::class.java.module.getResourceAsStream("detekt.yml").reader().readText()
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

val rootLibs get() = buildLogicBaseDeps.libs
