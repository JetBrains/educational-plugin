package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.json.migration.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.json.migration.TaskRoots
import com.jetbrains.edu.learning.json.migration.To10VersionLocalCourseConverter
import com.jetbrains.edu.learning.json.migration.To9VersionLocalCourseConverter
import com.jetbrains.edu.learning.json.mixins.deserializeTask
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import org.jetbrains.annotations.VisibleForTesting

private val LOG = logger<JacksonStepOptionsDeserializer>()

class JacksonStepOptionsDeserializer(vc: Class<*>? = null) : StdDeserializer<PyCharmStepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): PyCharmStepOptions {
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    val node: JsonNode = jp.codec.readTree(jp)
    val migratedNode = migrate(node as ObjectNode, JSON_FORMAT_VERSION)
    return objectMapper.treeToValue(migratedNode, PyCharmStepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    fun migrate(node: ObjectNode, maxVersion: Int): ObjectNode {
      var convertedStepOptions = node
      val versionJson = node.get(SerializationUtils.Json.FORMAT_VERSION)
      var version = 1
      if (versionJson != null) {
        version = versionJson.asInt()
      }
      while (version < maxVersion) {
        val converter = when (version) {
          1 -> ToSecondVersionJsonStepOptionsConverter()
          2 -> ToThirdVersionJsonStepOptionsConverter()
          3 -> ToFourthVersionJsonStepOptionsConverter()
          4 -> ToFifthVersionJsonStepOptionsConverter()
          5 -> ToSixthVersionJsonStepOptionConverter()
          6 -> ToSeventhVersionJsonStepOptionConverter()
          8 -> To9VersionJsonStepOptionConverter()
          9 -> To10VersionJsonStepOptionConverter()
          else -> null
        }
        if (converter != null) {
          convertedStepOptions = converter.convert(convertedStepOptions)
        }
        version++
      }
      node.put(SerializationUtils.Json.FORMAT_VERSION, maxVersion)
      return convertedStepOptions
    }
  }
}

class StepikReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val initialVersion = jsonObject.migrate(JSON_FORMAT_VERSION)

    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    val reply = objectMapper.treeToValue(jsonObject, Reply::class.java)
    // We need to save original version of reply object
    // to correct deserialize Reply#eduTask
    reply.version = initialVersion
    return reply
  }

  companion object {
    /**
     * Return object version before migration
     */
    @VisibleForTesting
    fun ObjectNode.migrate(maxVersion: Int): Int {
      val versionJson = get(SerializationUtils.Json.VERSION)
      if (versionJson == null && get(EDU_TASK) == null) {
        // solution doesn't contain any edu data, let's not migrate it
        return maxVersion
      }
      val initialVersion = versionJson?.asInt() ?: 1
      var version = initialVersion
      while (version < maxVersion) {
        when (version) {
          6 -> toSeventhVersion()
        }
        version++
      }
      put(SerializationUtils.Json.VERSION, maxVersion)
      return initialVersion
    }

    private fun ObjectNode.toSeventhVersion() {
      val solutionFiles = get("solution")
      if (solutionFiles == null) return
      if (solutionFiles.any { it.get(NAME).asText().endsWith(".py") }) return
      for (solutionFile in solutionFiles) {
        val value = solutionFile.get(NAME)?.asText()
        (solutionFile as ObjectNode).put(NAME, "src/$value")
      }
    }
  }
}

class JacksonSubmissionDeserializer(
  private val replyVersion: Int = JSON_FORMAT_VERSION,
  private val language: String? = null,
  vc: Class<*>? = null
) : StdDeserializer<Task>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Task? {
    val objectMapper = jp.codec
    val node: ObjectNode = objectMapper.readTree(jp) as ObjectNode
    node.migrate(replyVersion, JSON_FORMAT_VERSION, language)
    if (node.has(SerializationUtils.Json.TASK_TYPE)) {
      val taskType = node.get(SerializationUtils.Json.TASK_TYPE).asText()
      return when (taskType) {
        STRING_TASK_TYPE -> objectMapper.treeToValue(node, StringTask::class.java)
        NUMBER_TASK_TYPE -> objectMapper.treeToValue(node, NumberTask::class.java)
        else -> deserializeTask(node, taskType, objectMapper)
      }
    }
    LOG.warn("No task type found in json $node")
    return null
  }

  companion object {
    @VisibleForTesting
    fun ObjectNode.migrate(version: Int, maxVersion: Int, language: String?) {
      @Suppress("NAME_SHADOWING")
      var version = version
      while (version < maxVersion) {
        when (version) {
          1 -> toFifthVersion()
          6 -> toSeventhVersion(language)
          8 -> to9Version()
          9 -> to10Version()
        }
        version++
      }
    }

    private fun ObjectNode.toFifthVersion() {
      val taskTexts = get(SerializationUtils.Json.TASK_TEXTS)
      if (taskTexts != null && taskTexts.size() > 0) {
        val description = taskTexts.fields().next()?.value?.asText()
        put(SerializationUtils.Json.DESCRIPTION_TEXT, description)
      }
      put(SerializationUtils.Json.DESCRIPTION_FORMAT, DescriptionFormat.HTML.toString())
      remove(SerializationUtils.Json.TASK_TEXTS)
    }

    private fun ObjectNode.toSeventhVersion(language: String?) {
      val taskRoots = getTaskRoots(language)
      if (taskRoots != null) {
        val taskFiles = get(SerializationUtils.Json.TASK_FILES)
        if (taskFiles != null) {
          val convertedTaskFiles = ObjectMapper().createObjectNode()
          for ((path, taskFile) in taskFiles.fields()) {
            val convertedPath = "${taskRoots.taskFilesRoot}/$path"
            (taskFile as ObjectNode).put(NAME, convertedPath)
            convertedTaskFiles.set<JsonNode?>(convertedPath, taskFile)
          }
          set<JsonNode?>(SerializationUtils.Json.TASK_FILES, convertedTaskFiles)
        }

        val testFiles = get(SerializationUtils.Json.TEST_FILES)
        if (testFiles != null) {
          val convertedTestFiles = ObjectMapper().createObjectNode()
          for ((path, testFile) in testFiles.fields()) {
            convertedTestFiles.set<JsonNode?>("${taskRoots.testFilesRoot}/$path", testFile)
          }

          set<JsonNode?>(SerializationUtils.Json.TEST_FILES, convertedTestFiles)
        }
      }
    }

    private fun ObjectNode.to9Version() {
      To9VersionLocalCourseConverter.convertTaskObject(this)
    }

    private fun ObjectNode.to10Version() {
      To10VersionLocalCourseConverter.convertTaskObject(this)
    }
  }
}

private fun getTaskRoots(language: String?): TaskRoots? {
  if (language == null) return null
  return LANGUAGE_TASK_ROOTS[language]
}