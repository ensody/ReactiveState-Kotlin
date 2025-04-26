package com.ensody.buildlogic

import org.gradle.api.Project

val Project.isRootProject get() = this == rootProject

fun shell(command: String): String {
    val process = ProcessBuilder("/bin/bash", "-c", command).start()
    return process.inputStream.bufferedReader().readText().trim().also {
        process.waitFor()
    }
}

internal fun Project.detectProjectVersion(): String =
    System.getenv("OVERRIDE_VERSION")?.takeIf { it.isNotBlank() }
        ?: shell("git tag --points-at HEAD").split("\n").filter {
            versionRegex.matchEntire(it) != null
        }.maxByOrNull {
            VersionComparable(versionRegex.matchEntire(it)!!.destructured.toList())
        }?.removePrefix("v")?.removePrefix("-") ?: run {
            val branchName = shell("git rev-parse --abbrev-ref HEAD")
            "999999.0.0-${sanitizeBranchName(branchName)}.1"
        }

private class VersionComparable(val parts: List<String>) : Comparable<VersionComparable> {
    override fun compareTo(other: VersionComparable): Int {
        for ((l, r) in parts.zip(other.parts)) {
            val result = l.compareTo(r)
            if (result != 0) return result
        }
        return parts.size.compareTo(other.parts.size)
    }
}

private fun sanitizeBranchName(name: String): String =
    sanitizeRegex.replace(name, "-")

private val versionRegex = Regex("""v-?(\d+)\.(\d+)\.(\d+)((-.+?\.)(\d+))*""")
private val sanitizeRegex = Regex("""[^A-Za-z0-9\-]""")
