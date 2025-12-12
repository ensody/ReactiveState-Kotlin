package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType

fun Project.setupDetekt() {
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Enable type resolution
        classpath = detektClasspath
        jvmTarget = "17"

        config.from(rootProject.file("build/build-logic/detekt.yml"))
        buildUponDefaultConfig = true

        setSource(
            files(
                file("src").listFiles().filter {
                    it.name.endsWith("Main") || it.name.endsWith("Test") || it.name in setOf("main", "test")
                }.flatMap {
                    listOf(it.resolve("kotlin"), it.resolve("java"))
                },
            ),
        )
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "17"
    }
}
