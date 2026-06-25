package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

fun Project.setupPlatformProject() {
    extensions.getByType<JavaPlatformExtension>().allowDependencies()

    // Ensure all subprojects are evaluated so their publications are available
    rootProject.subprojects.forEach { subproject ->
        if (subproject != this) {
            evaluationDependsOn(subproject.path)
        }
    }

    val allConstraints = rootProject.subprojects.flatMap { subproject ->
        if (subproject == this) return@flatMap emptyList()
        if (!subproject.plugins.hasPlugin("maven-publish") ||
            subproject.plugins.hasPlugin("java-platform") ||
            subproject.plugins.hasPlugin("version-catalog")
        ) {
            return@flatMap emptyList()
        }
        subproject.extensions.findByType<PublishingExtension>()?.publications.orEmpty()
            .filterIsInstance<MavenPublication>()
            .filterNot {
                it.artifactId.endsWith("-metadata") || it.artifactId.endsWith("-kotlinMultiplatform")
            }.map { publication ->
                dependencies.constraints.create("${publication.groupId}:${publication.artifactId}:${publication.version}")
            }
    }

    configurations.named("api").get().dependencyConstraints.addAll(allConstraints)
}
