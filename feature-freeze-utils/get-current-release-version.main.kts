@file:Repository("https://repo.maven.apache.org/maven2/")

import java.io.File

val versionParam = args.getOrNull(0)?.trim()

// Read version from gradle.properties
val gradlePropertiesFile = File("gradle.properties")

val version = gradlePropertiesFile.readLines()
        .firstOrNull { it.trim().startsWith("pluginVersion=") }
        ?.substringAfter("=")
        ?.trim()

if (version != null) {
    // Set TeamCity parameter using service message
    println("##teamcity[setParameter name='$versionParam' value='$version']")
    println("Plugin version: $version")
} else {
    System.err.println("Could not determine project version")
    kotlin.system.exitProcess(1)
}
