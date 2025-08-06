package com.ensody.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

fun Project.setupKtLint(version: Provider<MinimalExternalModuleDependency>) {
    configurations.create("ktlint")

    dependencies {
        add("ktlint", version)
        // additional 3rd party ruleset(s) can be specified here
        // just add them to the classpath (e.g. ktlint 'groupId:artifactId:version') and
        // ktlint will pick them up
    }

    tasks.register<JavaExec>("ktlint") {
        group = "verification"
        description = "Check Kotlin code style."
        classpath = configurations.getByName("ktlint")
        mainClass.set("com.pinterest.ktlint.Main")
        args(
            "--log-level=warn",
            "src/*ain/**/*.kt",
            "src/*est/**/*.kt",
            "--reporter=plain",
            "--reporter=checkstyle,output=$projectDir/build/ktlint.xml",
        )
    }
    tasks.findByName("check")?.dependsOn("ktlint")

    tasks.register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fix Kotlin code style deviations."
        classpath = configurations.getByName("ktlint")
        mainClass.set("com.pinterest.ktlint.Main")
        args(
            "--log-level=warn",
            "-F",
            "src/*ain/**/*.kt",
            "src/*est/**/*.kt",
        )
    }
}
