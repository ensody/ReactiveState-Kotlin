@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import com.android.build.gradle.BaseExtension
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.net.URI

class BuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            pluginManager.apply("com.ensody.build-logic-base")
            if (!isRootProject) {
                if (name.endsWith("-bom")) {
                    pluginManager.apply("java-platform")
                } else {
                    pluginManager.apply("com.android.library")
                    pluginManager.apply("org.jetbrains.kotlin.multiplatform")
                    if ("-compose" in name) {
                        pluginManager.apply("org.jetbrains.compose")
                        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
                    }
                    pluginManager.apply("io.gitlab.arturbosch.detekt")
                }
                pluginManager.apply("maven-publish")
            }
            pluginManager.apply("org.jetbrains.dokka")
        }
    }
}

fun Project.initBuildLogic() {
    group = "com.ensody.reactivestate"

    initBuildLogicBase {
        setupRepositories()

        configure<NexusPublishExtension> {
            repositories {
                sonatype {
                    nexusUrl.set(URI("https://s01.oss.sonatype.org/service/local/"))
                    snapshotRepositoryUrl.set(URI("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                    username = System.getenv("PUBLICATION_USERNAME")
                    password = System.getenv("PUBLICATION_PASSWORD")
                }
            }
        }
    }
}

fun Project.setupRepositories() {
    repositories {
        google()
        mavenCentral()
        if (System.getenv("RUNNING_ON_CI") != "true") {
            mavenLocal()
        }
    }
}

fun Project.setupBuildLogic(block: Project.() -> Unit) {
    setupBuildLogicBase {
        setupRepositories()
        if (extensions.findByType<JavaPlatformExtension>() != null) {
            setupPlatformProject()
        }
        if (extensions.findByType<BaseExtension>() != null) {
            setupAndroid(coreLibraryDesugaring = libs.findLibrary("desugarJdkLibs").get())
        }
        if (extensions.findByType<KotlinMultiplatformExtension>() != null) {
            setupKmp {
                addAllTargets(onlyComposeSupport = project.name == "reactivestate-compose")
                compilerOptions {
                    optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn.add("kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi")
                    optIn.add("kotlinx.coroutines.FlowPreview")
                    optIn.add("com.ensody.reactivestate.ExperimentalReactiveStateApi")
                }
            }
            tasks.register("testAll") {
                group = "verification"
                dependsOn(
                    "testDebugUnitTest",
                    "jvmTest",
                    "iosSimulatorArm64Test",
                    "iosX64Test",
                    "macosArm64Test",
                    "macosX64Test",
                    "mingwX64Test",
                    "linuxX64Test",
//                    "linuxArm64Test",
//                    "tvosSimulatorArm64Test",
//                    "tvosX64Test",
//                    "watchosSimulatorArm64Test",
//                    "watchosX64Test",
//                    "jsIrNodeTest",
                )
            }
        }
        if (extensions.findByType<KotlinBaseExtension>() != null) {
            setupKtLint(libs.findLibrary("ktlint-cli").get())
        }
        if (extensions.findByType<DetektExtension>() != null) {
            setupDetekt()
        }
        if (extensions.findByType<DokkaExtension>() != null) {
            setupDokka(copyright = "Ensody GmbH")
        }
        if (extensions.findByType<PublishingExtension>() != null) {
            setupPublication(
                withJavadocJar = true,
                withSources = true,
                signingKeyInfo = SigningKeyInfo.loadFromEnvOrNull(),
            ) {
                pom {
                    description = "Kotlin Multiplatform ViewModels and reactive state management based on StateFlow"
                    url = "https://github.com/ensody/ReactiveState-Kotlin"
                    licenses {
                        apache2()
                    }
                    scm {
                        url.set(this@pom.url)
                    }
                    developers {
                        developer {
                            id = "wkornewald"
                            name = "Waldemar Kornewald"
                            url = "https://www.ensody.com"
                            organization = "Ensody GmbH"
                            organizationUrl = url
                        }
                    }
                }
            }
        }
        block()
    }
}
