@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

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
