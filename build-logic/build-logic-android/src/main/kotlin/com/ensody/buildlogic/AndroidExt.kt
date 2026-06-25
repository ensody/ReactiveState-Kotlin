package com.ensody.buildlogic

import com.android.build.api.withAndroid
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Packaging
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private val androidSdk = 36
private val androidMinSdk = 23

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun Project.setupAndroidLib(
    coreLibraryDesugaring: Provider<MinimalExternalModuleDependency>?,
) {
    configure<KotlinMultiplatformExtension> {
        applyKmpHierarchy {
            common {
                group("jvmCommon") {
                    withJvm()
                    withAndroid()
                }
            }
        }
        configure<KotlinMultiplatformAndroidLibraryTarget> {
            namespace = getDefaultPackageName()
            testNamespace = "$namespace.unittests"
            compileSdk {
                version = release(androidSdk)
            }
            minSdk {
                version = release(androidMinSdk)
            }

            enableCoreLibraryDesugaring = coreLibraryDesugaring != null
            if (coreLibraryDesugaring != null) {
                project.dependencies {
                    add("coreLibraryDesugaring", coreLibraryDesugaring)
                }
            }

            withHostTest {
                targetSdk {
                    version = release(androidSdk)
                }
                isIncludeAndroidResources = true
            }

            packaging {
                configurePackaging()
            }
        }
    }
}

fun Project.setupAndroidApp(
    coreLibraryDesugaring: Provider<MinimalExternalModuleDependency>?,
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
) {
    configure<ApplicationExtension> {
        namespace = getDefaultPackageName()
        testNamespace = "$namespace.unittests"
        compileSdk {
            version = release(androidSdk)
        }
        defaultConfig {
            minSdk = androidMinSdk
            targetSdk = androidSdk
            versionCode = 1
            versionName = project.version as String
            // Required for coreLibraryDesugaring
            multiDexEnabled = true
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = coreLibraryDesugaring != null
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        testOptions {
            // Needed for Robolectric
            unitTests {
                isIncludeAndroidResources = true
            }
        }

        packaging {
            configurePackaging()
        }
    }
    if (coreLibraryDesugaring != null) {
        dependencies {
            add("coreLibraryDesugaring", coreLibraryDesugaring)
        }
    }
}

private fun Packaging.configurePackaging() {
    resources {
        pickFirsts.add("META-INF/*.kotlin_module")
        pickFirsts.add("META-INF/AL2.0")
        pickFirsts.add("META-INF/LGPL2.1")
        pickFirsts.add("META-INF/**/MANIFEST.MF")
        pickFirsts.add("META-INF/LICENSE*.md")
    }
}
