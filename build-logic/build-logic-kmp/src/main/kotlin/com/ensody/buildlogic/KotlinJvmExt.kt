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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

fun Project.setupKotlinJvm(
    javaVersion: JavaVersion = JavaVersion.VERSION_17,
    block: KotlinJvmExtension.() -> Unit = {},
) {
    tasks.withType<Test> {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
    configure<KotlinJvmExtension> {
        explicitApi = ExplicitApiMode.Strict
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
        compilerOptions {
            allWarningsAsErrors.set(true)
            optIn.addAll(commonKotlinOptIns)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
        block()
    }
}
