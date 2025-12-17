import java.io.File
import kotlin.system.exitProcess

// Prefer command line argument for RELEASE_VERSION; fall back to environment variable for compatibility
val releaseVersion: String = args.firstOrNull() ?: run {
        println("Error: RELEASE_VERSION is required. Pass it as the first argument.\nUsage: kotlinc -script update-plugin-version-info.main.kts <RELEASE_VERSION>")
        exitProcess(1)
    }

fun extractVersion(filePath: String, constantName: String): String? {
    val file = File(filePath)
    if (!file.exists()) {
        println("Error: File not found at $filePath")
        return null
    }

    val regex = Regex("""(?:const\s+val|val)\s+$constantName\s*(?::\s*Int)?\s*=\s*(\d+)""")
    val match = regex.find(file.readText())

    return match?.groupValues?.get(1)
}

fun addVersionToTable(
    markdownPath: String,
    eduVersionsPath: String,
    yamlMapperPath: String,
    pluginVersion: String
) {
    // Extract JSON_FORMAT_VERSION from EduVersions.kt
    val jsonVersion = extractVersion(eduVersionsPath, "JSON_FORMAT_VERSION")
    if (jsonVersion == null) {
        println("Error: Could not extract JSON_FORMAT_VERSION from EduVersions.kt")
        return
    }

    // Extract CURRENT_YAML_VERSION from YamlMapper.kt
    val yamlVersion = extractVersion(yamlMapperPath, "CURRENT_YAML_VERSION")
    if (yamlVersion == null) {
        println("Error: Could not extract CURRENT_YAML_VERSION from YamlMapper.kt")
        return
    }

    val file = File(markdownPath)
    val content = file.readText()

    val tableHeader = "**Versions without EDU IDEs**"
    val nextTableHeader = "**Versions with EDU IDEs**"

    val tableStart = content.indexOf(tableHeader)
    if (tableStart == -1) {
        println("Error: Could not find '$tableHeader' in file")
        return
    }

    val tableEnd = content.indexOf(nextTableHeader, tableStart)
    if (tableEnd == -1) {
        println("Error: Could not find '$nextTableHeader' in file")
        return
    }

    val beforeTable = content.substring(0, tableStart)
    val tableSection = content.substring(tableStart, tableEnd)
    val afterTable = content.substring(tableEnd)
    val lines = tableSection.lines().toMutableList()

    // Check if pluginVersion already exists in the table
    if (lines.any { it.contains("| $pluginVersion") || it.contains("| ${pluginVersion.padEnd(9)}") }) {
        println("Version $pluginVersion already exists in the table. Skipping.")
        return
    }

    val insertIndex = lines.indexOfLast { it.trimStart().startsWith("|") && it.contains("|") }

    if (insertIndex == -1) {
        println("Error: Could not find table rows")
        return
    }

    val newRow = "| ${pluginVersion.padEnd(9)} | ${jsonVersion.padEnd(4)} | ${yamlVersion.padEnd(4)} |"

    lines.add(insertIndex + 1, newRow)

    val newContent = beforeTable + lines.joinToString("\n") + afterTable

    file.writeText(newContent)
    println("Successfully added version $pluginVersion (Json=$jsonVersion, Yaml=$yamlVersion) to the table")
}

//Main Execution

addVersionToTable(
    markdownPath = "documentation/PluginVersionsInfo.md",
    eduVersionsPath = "edu-format/src/com/jetbrains/edu/learning/courseFormat/EduVersions.kt",
    yamlMapperPath = "edu-format/src/com/jetbrains/edu/learning/yaml/YamlMapper.kt",
    pluginVersion = releaseVersion
)
