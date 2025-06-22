package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType

fun Project.setupPlatformProject() {
    extensions.getByType<JavaPlatformExtension>().allowDependencies()

    afterEvaluate {
        val allPublications = rootProject.subprojects.flatMap { subproject ->
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
                }.map {
                    subproject.dependencies.constraints.create("${it.groupId}:${it.artifactId}:${it.version}")
                }
        }

        configurations.named("api").get().apply {
            dependencyConstraints.addAll(allPublications)
        }
    }
}
