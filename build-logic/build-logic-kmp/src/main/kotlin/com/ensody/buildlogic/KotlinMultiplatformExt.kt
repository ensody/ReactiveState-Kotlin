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
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

fun Project.setupKmp(
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
    block: KotlinMultiplatformExtension.() -> Unit,
) {
    val commonMainDir = file("src/commonMain")
    if (!commonMainDir.exists() || commonMainDir.walkBottomUp().none { it.extension == "kt" }) {
        val packageName = getDefaultPackageName()
        withGeneratedBuildFile("empty", "${packageName.replace(".", "/")}/empty.kt", "commonMain") {
            """
            package $packageName

            // The Kotlin compiler doesn't like empty binaries
            // Workaround for https://youtrack.jetbrains.com/issue/KT-42702
            // and https://youtrack.jetbrains.com/issue/KT-47345
            internal val empty: Boolean = false
            """
        }
    }

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
            optIn.addAll(commonKotlinOptIns)
            freeCompilerArgs.addAll(commonKotlinCompilerArgs)
        }
        applyKmpHierarchy()
        tasks.withType<KotlinNativeCompile> {
            compilerOptions {
                optIn.add("kotlin.experimental.ExperimentalNativeApi")
                optIn.add("kotlinx.cinterop.BetaInteropApi")
                optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        block()
    }
}

val commonKotlinCompilerArgs = listOf(
    "-Xexpect-actual-classes",
    "-Xannotation-default-target=param-property",
)

val commonKotlinOptIns = listOf(
    "kotlin.RequiresOptIn",
    "kotlin.ExperimentalStdlibApi",
    "kotlin.ExperimentalUnsignedTypes",
    "kotlin.concurrent.atomics.ExperimentalAtomicApi",
    "kotlin.contracts.ExperimentalContracts",
    "kotlin.experimental.ExperimentalObjCRefinement",
    "kotlin.experimental.ExperimentalObjCName",
    "kotlin.io.encoding.ExperimentalEncodingApi",
    "kotlin.time.ExperimentalTime",
    "kotlin.uuid.ExperimentalUuidApi",
)

fun KotlinMultiplatformExtension.applyKmpHierarchy(block: KotlinHierarchyBuilder.Root.() -> Unit = {}) {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmCommon") {
                withJvm()
                withAndroidTarget()
            }
            group("desktop") {
                withLinux()
                withMingw()
                withMacos()
            }
            group("appleMobile") {
                withIos()
                group("ios")
                withTvos()
                withWatchos()
            }
            group("compose") {
                group("js")
                withJs()
                group("wasmJs")
                withWasmJs()
                withWasmWasi()
                group("ios")
                withIos()
                withJvm()
                withAndroidTarget()
            }
            group("nonJvm") {
                withNative()
                group("js")
                withJs()
                group("wasmJs")
                withWasmJs()
            }
            group("nonJs") {
                group("native")
                withNative()
                group("jvmCommon")
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
    addAllNonJsTargets(onlyComposeSupport = onlyComposeSupport, iosX64 = iosX64)
    allJs()
}

fun KotlinMultiplatformExtension.addAllNonJsTargets(
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
    allAppleMobile(x64 = iosX64, onlyComposeSupport = onlyComposeSupport)
    allDesktop()
}

fun KotlinMultiplatformExtension.allDesktop() {
    allMacos()
    allLinux()
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

fun KotlinMultiplatformExtension.allAppleMobile(x64: Boolean = true, onlyComposeSupport: Boolean = false) {
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
