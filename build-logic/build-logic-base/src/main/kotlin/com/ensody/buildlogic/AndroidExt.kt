package com.ensody.buildlogic

import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

fun Project.setupAndroid(
    coreLibraryDesugaring: Provider<MinimalExternalModuleDependency>?,
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
) {
    configure<TestedExtension> {
        namespace = getDefaultPackageName()
        testNamespace = "$namespace.unittests"
        val sdk = 35
        compileSdkVersion(sdk)
        defaultConfig {
            minSdk = 21
            targetSdk = sdk
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

        packagingOptions {
            resources {
                pickFirsts.add("META-INF/*.kotlin_module")
                pickFirsts.add("META-INF/AL2.0")
                pickFirsts.add("META-INF/LGPL2.1")
            }
        }
    }
    if (coreLibraryDesugaring != null) {
        dependencies {
            add("coreLibraryDesugaring", coreLibraryDesugaring)
        }
    }
}
