@file:Repository("https://repo.maven.apache.org/maven2/")

import java.io.File

// 1) Read and validate input
val masterVersion = args.getOrNull(0)?.trim()
if (masterVersion.isNullOrEmpty()) {
    error("Error: MASTER_VERSION argument is required")
}

// 4) Locate gradle.properties in repo root
val propFile = File("gradle.properties")
if (!propFile.exists()) {
    error("Error: gradle.properties not found in repo root")
}

// 5) Read current pluginVersion
val lines = propFile.readLines().toMutableList()
val idx = lines.indexOfFirst { it.trimStart().startsWith("pluginVersion=") }
val currentVersion = if (idx >= 0) lines[idx].substringAfter("=").trim() else null

if (currentVersion == masterVersion) {
    println("pluginVersion is already set to $masterVersion, nothing to do")
    kotlin.system.exitProcess(0)
}

// 6) Update or append pluginVersion
if (idx >= 0) {
    val prefix = lines[idx].substringBefore("=")
    lines[idx] = "$prefix=$masterVersion"
} else {
    lines.add("pluginVersion=$masterVersion")
}

propFile.writeText(lines.joinToString(System.lineSeparator()))
