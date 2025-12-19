import java.io.File
import kotlin.system.exitProcess

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
  pluginVersion: String,
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
  if (lines.any { it.contains("| $pluginVersion") }) {
    println("Version $pluginVersion already exists in the table. Skipping.")
    return
  }

  val insertIndex = lines.indexOfLast { it.trimStart().startsWith("|") && it.contains("|") }

  if (insertIndex == -1) {
    println("Error: Could not find table rows")
    return
  }

  // Expects row in format: | 2025.11   | 22   | 5    |
  val columns = lines[insertIndex].split("|").drop(1)
  var releasePadEnd = 10
  var jsonPadEnd = 5
  var yamlPadEnd = 5
  if (columns.size == 3) {
    releasePadEnd = columns[0].length - 1
    jsonPadEnd = columns[1].length - 1
    yamlPadEnd = columns[2].length - 1
  }
  val newRow = "| ${pluginVersion.padEnd(releasePadEnd)}| ${jsonVersion.padEnd(jsonPadEnd)}| ${yamlVersion.padEnd(yamlPadEnd)}|"

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
