package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.converter.TaskRoots
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.serialization.converter.json.local.To10VersionLocalCourseConverter
import com.jetbrains.edu.learning.serialization.converter.json.local.To9VersionLocalCourseConverter
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions

private val LOG = Logger.getInstance(EduUtils::class.java)

class JacksonStepOptionsDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<PyCharmStepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): PyCharmStepOptions {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val node: JsonNode = jp.codec.readTree(jp)
    val migratedNode = migrate(node as ObjectNode, JSON_FORMAT_VERSION)
    return objectMapper.treeToValue(migratedNode, PyCharmStepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    @JvmStatic
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

class StepikReplyDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val initialVersion = jsonObject.migrate(JSON_FORMAT_VERSION)

    val objectMapper = StepikConnector.createMapper(SimpleModule())
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
    @JvmStatic
    fun ObjectNode.migrate(maxVersion: Int): Int {
      val initialVersion = get(SerializationUtils.Json.VERSION)?.asInt() ?: 1
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

class JacksonSubmissionDeserializer @JvmOverloads constructor(private val replyVersion: Int = JSON_FORMAT_VERSION,
                                                              private val language: String? = null,
                                                              vc: Class<*>? = null) : StdDeserializer<Task>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Task? {
    val module = SimpleModule()
    module.addDeserializer(AnswerPlaceholder::class.java, StepikSubmissionAnswerPlaceholderDeserializer(replyVersion, language))
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    node.migrate(replyVersion, JSON_FORMAT_VERSION, language)
    return doDeserializeTask(node, jp.codec)
  }

  companion object {
    @VisibleForTesting
    @JvmStatic
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
            (taskFile as ObjectNode).put(SerializationUtils.Json.NAME, convertedPath)
            convertedTaskFiles.set(convertedPath, taskFile)
          }
          set(SerializationUtils.Json.TASK_FILES, convertedTaskFiles)
        }

        val testFiles = get(SerializationUtils.Json.TEST_FILES)
        if (testFiles != null) {
          val convertedTestFiles = ObjectMapper().createObjectNode()
          for ((path, testFile) in testFiles.fields()) {
            convertedTestFiles.set("${taskRoots.testFilesRoot}/$path", testFile)
          }

          set(SerializationUtils.Json.TEST_FILES, convertedTestFiles)
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

private class StepikSubmissionAnswerPlaceholderDeserializer @JvmOverloads constructor(
  private val replyVersion: Int,
  private val language: String?,
  vc: Class<*>? = null) : StdDeserializer<AnswerPlaceholder>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): AnswerPlaceholder? {
    val placeholderObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    placeholderObject.migrate(replyVersion, language)
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val placeholder = objectMapper.treeToValue(placeholderObject, AnswerPlaceholder::class.java)

    if (placeholderObject.has(SerializationUtils.Json.SELECTED)) {
      placeholder.selected = placeholderObject.get(SerializationUtils.Json.SELECTED).asBoolean()
    }

    if (placeholderObject.has(SerializationUtils.STATUS)) {
      placeholder.status = CheckStatus.valueOf(placeholderObject.get(SerializationUtils.STATUS).asText())
    }

    return placeholder
  }

  companion object {
    private fun ObjectNode.migrate(version: Int, language: String?) {
      @Suppress("NAME_SHADOWING")
      var version = version
      while (version < JSON_FORMAT_VERSION) {
        when (version) {
          1 -> ToFifthVersionJsonStepOptionsConverter.removeSubtaskInfo(this)
          6 -> {
            val taskFilesRoot = getTaskRoots(language)?.taskFilesRoot
            if (taskFilesRoot != null) {
              ToSeventhVersionJsonStepOptionConverter.convertPlaceholder(this, taskFilesRoot)
            }
          }
        }
        version++
      }
    }
  }
}

fun doDeserializeTask(node: ObjectNode, objectMapper: ObjectCodec): Task? {
  if (node.has(SerializationUtils.Json.TASK_TYPE)) {
    val taskType = node.get(SerializationUtils.Json.TASK_TYPE).asText()
    return when (taskType) {
      "ide" -> objectMapper.treeToValue(node, IdeTask::class.java)
      "choice" -> objectMapper.treeToValue(node, ChoiceTask::class.java)
      "theory" -> objectMapper.treeToValue(node, TheoryTask::class.java)
      "code" -> objectMapper.treeToValue(node, CodeTask::class.java)
      "edu" -> objectMapper.treeToValue(node, EduTask::class.java)
      "output" -> objectMapper.treeToValue(node, OutputTask::class.java)
      "pycharm" -> objectMapper.treeToValue(node, EduTask::class.java)     // deprecated: old courses have pycharm tasks
      else -> {
        LOG.warn("Unsupported task type $taskType")
        null
      }
    }
  }
  LOG.warn("No task type found in json $node")
  return null
}

private fun getTaskRoots(language: String?): TaskRoots? {
  if (language == null) return null
  return LANGUAGE_TASK_ROOTS[language]
}