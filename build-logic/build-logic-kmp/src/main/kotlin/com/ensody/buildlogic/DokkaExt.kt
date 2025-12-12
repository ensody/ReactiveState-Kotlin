package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import java.time.LocalDate

fun Project.setupDokka(copyright: String) {
    extra.set(dokkaDoneMarker, true)

    if (!isRootProject) {
        if (!rootProject.extra.has(dokkaDoneMarker)) {
            rootProject.setupDokka(copyright = copyright)
        }
        rootProject.dependencies {
            add("dokka", this@setupDokka)
        }
    }
    configure<DokkaExtension> {
        dokkaSourceSets.configureEach {
            enableAndroidDocumentationLink.set(true)
            includes.from(
                *fileTree(projectDir) {
                    includes.addAll(listOf("index.md", "README.md", "Module.md", "docs/*.md"))
                }.files.toTypedArray(),
            )
        }
        pluginsConfiguration.withType<DokkaHtmlPluginParameters> {
            footerMessage.set("Copyright © ${LocalDate.now().year} $copyright")
        }
        if (isRootProject) {
            dokkaPublications.configureEach {
                includes.from(rootProject.file("docs/README.md"))
                outputDirectory.set(rootProject.file("build/docs/$name"))
            }
        }
    }
}

private val dokkaDoneMarker = "_dokka_setup_done"
