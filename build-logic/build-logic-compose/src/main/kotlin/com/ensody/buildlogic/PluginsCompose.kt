@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

// NOTE: The following plugins get registered based on their class name prefix as com.ensody.build-logic.<prefix>

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
