@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

package com.ensody.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder

fun Project.setupKmp(
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
    pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    tasks.withType<Test> {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    configure<KotlinMultiplatformExtension> {
        explicitApi = ExplicitApiMode.Strict
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
        compilerOptions {
            allWarningsAsErrors.set(true)
            optIn.add("kotlin.RequiresOptIn")
            optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
            optIn.add("kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi")
            optIn.add("kotlinx.coroutines.FlowPreview")
            optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
            optIn.add("com.ensody.reactivestate.ExperimentalReactiveStateApi")
            freeCompilerArgs.add("-Xexpect-actual-classes")
//            freeCompilerArgs.add("-Xexplicit-api=strict")
        }
        applyKmpHierarchy()
        block()
    }
}

fun KotlinMultiplatformExtension.applyKmpHierarchy(block: KotlinHierarchyBuilder.Root.() -> Unit = {}) {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withJvm()
                withAndroidTarget()
            }
            group("compose") {
                withJs()
                withWasmJs()
                withWasmWasi()
                withIos()
                withMacos()
                withJvm()
                withAndroidTarget()
            }
            group("nonJvm") {
                withNative()
                withJs()
                withWasmJs()
            }
            group("nonJs") {
                withNative()
                withJvm()
                withAndroidTarget()
            }
        }
        block()
    }
}

fun KotlinMultiplatformExtension.addAllTargets(
    onlyComposeSupport: Boolean = false,
    iosX64: Boolean = true,
) {
    androidTarget {
        publishLibraryVariants("release")
    }
    if (!onlyComposeSupport) {
        allAndroidNative()
    }
    jvm()
    allJs()
    allAppleMobile(x64 = iosX64, onlyComposeSupport = onlyComposeSupport)
    allDesktop()
}

fun KotlinMultiplatformExtension.allDesktop() {
    allMacos()
    allLinux()
    allMacos()
    mingwX64()
}

fun KotlinMultiplatformExtension.allLinux() {
    linuxX64()
    linuxArm64()
}

fun KotlinMultiplatformExtension.allMacos() {
    macosArm64()
    macosX64()
}

fun KotlinMultiplatformExtension.allAndroidNative() {
    androidNativeArm64()
    androidNativeArm32()
    androidNativeX64()
    androidNativeX86()
}

fun KotlinMultiplatformExtension.allAppleMobile(x64: Boolean = true, onlyComposeSupport: Boolean) {
    allIos(x64 = x64)
    allTvos()
    allWatchos(onlyComposeSupport = onlyComposeSupport)
}

fun KotlinMultiplatformExtension.allIos(x64: Boolean = true) {
    iosArm64()
    iosSimulatorArm64()
    if (x64) {
        iosX64()
    }
}

fun KotlinMultiplatformExtension.allTvos() {
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
}

fun KotlinMultiplatformExtension.allWatchos(onlyComposeSupport: Boolean) {
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    if (!onlyComposeSupport) {
        watchosDeviceArm64()
    }
}

fun KotlinMultiplatformExtension.allJs() {
    js(IR) {
        browser()
        nodejs()
    }

    wasmJs {
        browser()
        nodejs()
    }

//    wasmWasi {
//        nodejs()
//    }
}
