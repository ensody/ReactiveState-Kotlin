@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

fun Project.setupGradlePlugin(kotlinVersionForGradlePlugins: String) {
    configure<KotlinJvmExtension> {
        compilerOptions {
            // TODO: Only disable deprecation warnings
            allWarningsAsErrors.set(false) // We access a deprecated Kotlin version number which causes a warning
            val kotlinVersion = KotlinVersion.fromVersion(
                kotlinVersionForGradlePlugins.split(".").take(2).joinToString("."),
            )
            apiVersion.set(kotlinVersion)
            languageVersion.set(kotlinVersion)
        }
        coreLibrariesVersion = kotlinVersionForGradlePlugins
    }
}
