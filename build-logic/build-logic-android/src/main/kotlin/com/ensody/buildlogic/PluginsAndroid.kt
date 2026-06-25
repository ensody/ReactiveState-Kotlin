@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

// NOTE: The following plugins get registered based on their class name prefix as com.ensody.build-logic.<prefix>

/** Android lib setup. */
class AndroidLibBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("com.android.kotlin.multiplatform.library")
            }
            pluginManager.apply("com.ensody.build-logic.kmp")
        }
    }
}

/** Android app setup. */
class AndroidAppBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("com.android.application")
            }
        }
    }
}
