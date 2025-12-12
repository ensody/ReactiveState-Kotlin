@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.ensody.buildlogic

import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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
