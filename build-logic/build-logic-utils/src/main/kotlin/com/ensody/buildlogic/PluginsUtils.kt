@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

// NOTE: The following plugins get registered based on their class name prefix as com.ensody.build-logic.<prefix>

class UtilsBuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}
