package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.api.plugins.catalog.CatalogPluginExtension
import org.gradle.kotlin.dsl.configure

fun Project.setupVersionCatalog() {
    // Workaround for https://github.com/gradle/gradle/issues/33568
    gradle.taskGraph.whenReady {
        configure<CatalogPluginExtension> {
            versionCatalog {
                version("kotlin", rootLibs.findVersion("kotlin").get().toString())
                val versionAlias = version(rootProject.name.lowercase(), project.version.toString())
                for (subproject in rootProject.subprojects) {
                    if (!subproject.plugins.hasPlugin("maven-publish") ||
                        subproject.plugins.hasPlugin("version-catalog")
                    ) {
                        continue
                    }
                    library(
                        subproject.name.substringAfter("-"),
                        subproject.group.toString(),
                        subproject.name,
                    ).versionRef(versionAlias)
                }
            }
        }
    }
}
