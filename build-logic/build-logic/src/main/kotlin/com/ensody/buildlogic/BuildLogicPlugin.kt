@file:Suppress("UnstableApiUsage")

package com.ensody.buildlogic

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import java.net.URI

class BuildLogicPlugin : Plugin<Project> {
    override fun apply(target: Project) {}
}

fun Project.initBuildLogic() {
    group = "com.ensody.reactivestate"

    initBuildLogicBase()
    allprojects {
        setupBuildLogic()
    }

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

fun Project.setupBuildLogic() {
    pluginManager.apply("com.ensody.build-logic")

    repositories {
        google()
        mavenCentral()
        if (System.getenv("RUNNING_ON_CI") != "true") {
            mavenLocal()
        }
    }

    if (isRootProject) return

    setupBuildLogicBase()

    if (project.name.endsWith("-bom")) {
        setupPlatformProject()
    } else {
        setupAndroid(coreLibraryDesugaring = libs.findLibrary("desugarJdkLibs").get())
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
//                "linuxArm64Test",
//                "tvosSimulatorArm64Test",
//                "tvosX64Test",
//                "watchosSimulatorArm64Test",
//                "watchosX64Test",
//                "jsIrNodeTest",
            )
        }

        dependencies {
            add("commonMainApi", platform(libs.findLibrary("coroutines-bom").get()))
        }

        setupKtLint(libs.findLibrary("ktlint-cli").get())
        setupDokka(copyright = "Ensody GmbH")
    }

    setupPublication(
        withJavadocJar = true,
        withSources = true,
        signingKeyInfo = SigningKeyInfo.loadFromEnvOrNull(),
    ) {
        pom {
            name = "${rootProject.name}: ${project.name}"
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
