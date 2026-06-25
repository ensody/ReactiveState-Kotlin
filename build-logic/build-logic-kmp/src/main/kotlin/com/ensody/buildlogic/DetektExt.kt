package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType

fun Project.setupDetekt() {
    if (file("src").walkBottomUp().none { it.isFile && it.extension == "kt" }) {
        return
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Enable type resolution
        classpath = detektClasspath
        jvmTarget = "21"

        config.from(rootProject.file("build/build-logic/detekt.yml"))
        buildUponDefaultConfig = true

        setSource(
            files(
                file("src").listFiles().orEmpty().filter {
                    it.name.endsWith("Main") || it.name.endsWith("Test") || it.name in setOf("main", "test")
                }.flatMap {
                    listOf(it.resolve("kotlin"), it.resolve("java"))
                },
            ),
        )
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "21"
    }
}
