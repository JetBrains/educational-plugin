@file:Suppress("UNCHECKED_CAST")

import Changes_gradle.JsonNode
import groovy.json.JsonSlurper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

extra["generateNotes"] = ::generateNotes

val name = "name"
val value = "value"
val YT = "https://youtrack.jetbrains.com"
val issuesApi = "$YT/api/issues"
val summary = "summary"
val numberInProject = "numberInProject"
val customFields = "customFields"
val bannedSubsystems = setOf("Help", "Product Management")
val indent = " ".repeat(4)
val eduPrefix = "EDU"

typealias JsonNode = Map<String, Any>

fun <T : Any> JsonNode.value(key: String): T {
  return this[key] as T
}

class Issue(val subsystem: String, val type: String, val state: String, summary: String, number: Int) {
  private val presentableNumber: String = "${eduPrefix}-${number}"
  val linkSource: String = "${YT}/issue/$presentableNumber"
  private val link: String = """<a href="$linkSource">$presentableNumber</a>"""
  val changeNote: String = "$link ($type): $summary"
}

fun createIssue(content: String): Issue? {
  val parsedIssue = JsonSlurper().parseText(content) as JsonNode

  val fields = parsedIssue.value<List<JsonNode>>(customFields)

  fun getCustomField(fieldName: String): String {
    val field = fields.find { it[name] == fieldName }!!
    return field.value<JsonNode>(value).value(name)
  }

  val subsystem = getCustomField("Subsystem")

  return if (subsystem in bannedSubsystems) null
  else Issue(subsystem,
             getCustomField("Type"),
             getCustomField("State"),
             parsedIssue[summary] as String,
             parsedIssue[numberInProject] as Int)
}

fun load(url: String): String = java.net.URL(url).readText()

fun subsystemWeight(subsystem: String): Int = when (subsystem) {
  "JetBrains Academy" -> Integer.MIN_VALUE
  "Marketplace" -> Integer.MIN_VALUE + 1
  else -> Integer.MAX_VALUE
}

fun typeWeight(issue: Issue): Int = when (issue.type) {
  "Feature" -> Integer.MIN_VALUE
  "Bug" -> Integer.MAX_VALUE
  else -> 0
}

fun generateNotes(version: String): String {
  val query = "project: {EduTools Plugin} Fix versions: ${version}"
  val issuesIds = JsonSlurper().parseText(load("$issuesApi?query=${URLEncoder.encode(query, UTF_8)}")) as List<JsonNode>

  val issues = issuesIds.mapNotNull {
    createIssue(load("$issuesApi/${it["id"]}?fields=${summary},$numberInProject,${customFields}($name,$value($name))"))
  }

  val groupedBySubsystem = issues.groupBy { it.subsystem }

  return buildString {
    appendln("<b>${version}</b>")
    appendln("<ul>")
    for (subsystem in groupedBySubsystem.keys.sortedBy(::subsystemWeight)) {
      appendln("$indent<li>$subsystem:")
      appendln("$indent$indent<ul>")
      val issuesInSubsystem = groupedBySubsystem[subsystem]!!
      for (issue in issuesInSubsystem.sortedBy(::typeWeight)) {
        appendln("""$indent$indent$indent<li>${issue.changeNote}</li>""")
        if (issue.state != "Fixed") {
          logger.warn("Open issue in change notes, needs checking if it should be included: ${issue.linkSource}")
        }
      }
      appendln("$indent$indent</ul>")
      appendln("$indent</li>")
    }
    appendln("</ul>")
  }
}
