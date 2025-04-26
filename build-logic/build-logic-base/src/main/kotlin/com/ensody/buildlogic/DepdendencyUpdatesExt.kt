package com.ensody.buildlogic

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

fun Project.setupDependencyUpdates() {
    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = Regex("""[0-9,.v-]+(-r)?""")
    return !stableKeyword && regex.matchEntire(version) == null
}
