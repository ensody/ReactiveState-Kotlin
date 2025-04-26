@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.ensody.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.setupPublication(
    withJavadocJar: Boolean,
    withSources: Boolean,
    signingKeyInfo: SigningKeyInfo? = null,
    block: MavenPublication.() -> Unit = {},
) {
    plugins.apply("maven-publish")

    if (pluginManager.hasPlugin("java-platform")) {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components.getByName("javaPlatform"))
                }
            }
        }
    } else {
        extensions.findByType<LibraryExtension>()?.apply {
            publishing {
                multipleVariants {
                    if (withJavadocJar) {
                        withJavadocJar()
                    }
                    allVariants()
                }
            }
        }

        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            publishing {
                withSourcesJar(withSources)
            }
        }

        if (withJavadocJar) {
            val emptyJar = tasks.register<Jar>("emptyJar") {
                archiveAppendix = "empty"
            }
            configure<PublishingExtension> {
                publications.withType<MavenPublication> {
                    artifact(emptyJar) { classifier = "javadoc" }
                }
            }
        }
    }

    afterEvaluate {
        configure<PublishingExtension> {
            publications.filterIsInstance<MavenPublication>().forEach {
                it.block()
            }
        }
    }

    if (signingKeyInfo != null) {
        plugins.apply("signing")
        configure<SigningExtension> {
            useInMemoryPgpKeys(signingKeyInfo.keyId, signingKeyInfo.keyRing, signingKeyInfo.keyPassword)
        }
    }
    if (plugins.hasPlugin("signing")) {
        configure<SigningExtension> {
            sign(extensions.getByType<PublishingExtension>().publications)
        }
        tasks.withType<AbstractPublishToMaven>().configureEach { mustRunAfter(tasks.withType<Sign>()) }
    }
}

class SigningKeyInfo(val keyId: String, val keyRing: String, val keyPassword: String) {
    companion object {
        fun loadFromEnvOrNull(
            keyIdEnv: String = "SIGNING_KEY_ID",
            keyRingEnv: String = "SIGNING_KEY_RING",
            keyPasswordEnv: String = "SIGNING_PASSWORD",
        ): SigningKeyInfo? {
            val keyId = System.getenv(keyIdEnv)
            val keyRing = System.getenv(keyRingEnv)
            val keyPassword = System.getenv(keyPasswordEnv)
            if (keyId?.isNotBlank() == true && keyRing?.isNotBlank() == true && keyPassword?.isNotBlank() == true) {
                return SigningKeyInfo(keyId, keyRing, keyPassword)
            }
            return null
        }
    }
}

fun MavenPomLicenseSpec.apache2() {
    license {
        name = "The Apache Software License, Version 2.0"
        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    }
}

fun MavenPomLicenseSpec.mit() {
    license {
        name = "The MIT License"
        url = "https://opensource.org/licenses/mit-license.php"
    }
}
