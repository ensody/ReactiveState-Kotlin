@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// NOTE: The following plugins get registered based on their class name prefix as com.ensody.build-logic.<prefix>

/** KMP setup. */
class AndroidBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            if (!isRootProject) {
                pluginManager.apply("com.android.library")
            }
            pluginManager.apply("com.ensody.build-logic.kmp")
        }
    }
}
