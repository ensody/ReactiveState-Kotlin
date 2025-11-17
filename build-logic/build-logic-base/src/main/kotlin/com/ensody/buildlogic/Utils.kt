package com.ensody.buildlogic

import com.android.build.gradle.internal.cxx.io.writeTextIfDifferent
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

val Project.isRootProject get() = this == rootProject

fun cli(
    vararg command: String,
    workingDir: File? = null,
    env: Map<String, String> = emptyMap(),
    inheritIO: Boolean = false,
): String {
    var cmd = command.first().replace("/", File.separator)
    if (OS.current == OS.Windows) {
        if (File("$cmd.bat").exists()) {
            cmd += ".bat"
        } else if (File("$cmd.exe").exists()) {
            cmd += ".exe"
        }
    }
    val processBuilder = ProcessBuilder(cmd, *command.drop(1).toTypedArray())
    workingDir?.let { processBuilder.directory(it) }
    processBuilder.redirectErrorStream(true)
    processBuilder.environment().putAll(env)
    val process = processBuilder.start()
    return process.inputStream.bufferedReader().readText().trim().also {
        val exitCode = process.waitFor()
        if (inheritIO) {
            println(it)
        }
        check(exitCode == 0) { "Process exit code was: $exitCode\nOriginal command: ${command.toList()}\nResult:$it" }
    }
}

fun shell(
    command: String,
    workingDir: File? = null,
    env: Map<String, String> = emptyMap(),
    inheritIO: Boolean = false,
): String =
    cli("/bin/bash", "-c", command, workingDir = workingDir, env = env, inheritIO = inheritIO)

fun Project.withGeneratedBuildFile(category: String, path: String, sourceSet: String? = null, content: () -> String) {
    val generatedDir = file("${getGeneratedBuildFilesRoot()}/$category")
    extensions.findByType<KotlinJvmExtension>()?.apply {
        sourceSets[sourceSet ?: "main"].kotlin.srcDir(generatedDir)
    } ?: extensions.findByType<KotlinMultiplatformExtension>()?.apply {
        sourceSets[sourceSet ?: "commonMain"].kotlin.srcDir(generatedDir)
    } ?: error("Don't know how to add generated build file because project ${this.path} has unknown type")
    val outputPath = file("$generatedDir/$path")
    outputPath.writeTextIfDifferent(content().trimIndent().trimStart() + "\n")
    generatedFiles.getOrPut(this.path) { mutableSetOf() }.add(outputPath.normalize().absoluteFile)
}

fun Project.getDefaultPackageName(): String =
    group.toString().split(".").let { prefix ->
        prefix + name.split("-").dropWhile { it == prefix.last() }
    }.joinToString(".")

internal val generatedFiles = mutableMapOf<String, MutableSet<File>>()

internal fun File.withParents(): List<File> =
    buildList {
        add(this@withParents)
        while (true) {
            add(last().parentFile ?: break)
        }
    }

internal fun Project.getGeneratedBuildFilesRoot(): File =
    file("$projectDir/build/generated/source/build-logic")

internal fun Project.detectProjectVersion(): String =
    System.getenv("OVERRIDE_VERSION")?.removePrefix("v")?.removePrefix("-")?.takeIf { it.isNotBlank() }
        ?: cli("git", "tag", "--points-at", "HEAD").split("\n").filter {
            versionRegex.matchEntire(it) != null
        }.maxByOrNull {
            VersionComparable(versionRegex.matchEntire(it)!!.destructured.toList())
        }?.removePrefix("v")?.removePrefix("-")?.takeIf { System.getenv("RUNNING_ON_CI") == "true" }
        ?: run {
            val branchName = cli("git", "rev-parse", "--abbrev-ref", "HEAD")
            "0.0.-${sanitizeBranchName(branchName)}.1"
        }

enum class OS {
    Linux,
    macOS,
    Windows,
    ;

    companion object Companion {
        val current: OS by lazy {
            val osName = System.getProperty("os.name").lowercase()
            when {
                "mac" in osName || "darwin" in osName -> macOS
                "linux" in osName -> Linux
                "windows" in osName -> Windows
                else -> error("Unknown operating system: $osName")
            }
        }
    }
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

private val versionRegex = Regex("""v-?(\d+)\.(\d+)\.(\d+)(((?:-.+?)?\.)(\d+))*""")
private val sanitizeRegex = Regex("""[^A-Za-z0-9\-]""")
