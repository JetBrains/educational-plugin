package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TASK_TYPE
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.converter.TaskRoots
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.StepOptions
import com.jetbrains.edu.learning.stepik.StepikNames

class JacksonLessonDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Lesson>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Lesson {
    val node: JsonNode = jp.codec.readTree(jp)
    val objectMapper = StepikConnector.getMapper(SimpleModule())
    val lesson = objectMapper.treeToValue(node, Lesson::class.java)
    val name = lesson.name
    if (StepikNames.PYCHARM_ADDITIONAL == name) {
      lesson.name = EduNames.ADDITIONAL_MATERIALS
    }
    return lesson
  }
}

class JacksonStepOptionsDeserializer @JvmOverloads constructor(var language: String? = null,
                                                               vc: Class<*>? = null) : StdDeserializer<StepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): StepOptions {
    val objectMapper = StepikConnector.getMapper(SimpleModule())
    val node: JsonNode = jp.codec.readTree(jp)
    val migratedNode = migrate(node as ObjectNode, JSON_FORMAT_VERSION, language)
    return objectMapper.treeToValue(migratedNode, StepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    @JvmOverloads
    @JvmStatic
    fun migrate(node: ObjectNode, maxVersion: Int, language: String? = null): ObjectNode {
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
          6 -> ToSeventhVersionJsonStepOptionConverter(language)
          8 -> To9VersionJsonStepOptionConverter()
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

class StepikReplyDeserializer @JvmOverloads constructor(private val language: String?, vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val initialVersion = jsonObject.migrate(JSON_FORMAT_VERSION, language)

    val objectMapper = StepikConnector.getMapper(SimpleModule())
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
    fun ObjectNode.migrate(maxVersion: Int, language: String?): Int {
      val initialVersion = get(SerializationUtils.Json.VERSION)?.asInt() ?: 1
      var version = initialVersion
      while (version < maxVersion) {
        when (version) {
          6 -> toSeventhVersion(language)
        }
        version++
      }
      put(SerializationUtils.Json.VERSION, maxVersion)
      return initialVersion
    }

    private fun ObjectNode.toSeventhVersion(language: String?) {
      val taskFilesRoot = getTaskRoots(language)?.taskFilesRoot
      if (taskFilesRoot != null) {
        for (solutionFile in get("solution")) {
          val value = solutionFile.get(NAME)?.asText()
          (solutionFile as ObjectNode).put(NAME, "$taskFilesRoot/$value")
        }
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
    val objectMapper = StepikConnector.getMapper(module)
    val node: JsonNode = objectMapper.readTree(jp)
    (node as ObjectNode).migrate(replyVersion, JSON_FORMAT_VERSION, language)
    return doDeserialize(node, objectMapper)
  }

  private fun doDeserialize(node: ObjectNode, objectMapper: ObjectMapper): Task? {
    if (node.has(NAME) && StepikNames.PYCHARM_ADDITIONAL == node.get(NAME).asText()) {
      node.remove(NAME)
      node.put(NAME, EduNames.ADDITIONAL_MATERIALS)
    }
    if (node.has(TASK_TYPE)) {
      val taskType = node.get(TASK_TYPE).asText()
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
    LOG.warn("No task type found in json " + node.toString())
    return null
  }

  companion object {
    private val LOG = Logger.getInstance(JacksonSubmissionDeserializer::class.java)
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
      put(SerializationUtils.Json.DESCRIPTION_FORMAT, DescriptionFormat.HTML.toString().toLowerCase())
      remove(SerializationUtils.Json.TASK_TEXTS)
    }

    private fun ObjectNode.toSeventhVersion(language: String?) {
      val taskRoots = getTaskRoots(language)
      if (taskRoots != null) {
        val taskFiles = get(SerializationUtils.Json.TASK_FILES)
        val convertedTaskFiles = ObjectMapper().createObjectNode()
        for ((path, taskFile) in taskFiles.fields()) {
          val convertedPath = "${taskRoots.taskFilesRoot}/$path"
          (taskFile as ObjectNode).put(SerializationUtils.Json.NAME, convertedPath)
          convertedTaskFiles.set(convertedPath, taskFile)
        }

        set(SerializationUtils.Json.TASK_FILES, convertedTaskFiles)

        val testFiles = get(SerializationUtils.Json.TEST_FILES)
        val convertedTestFiles = ObjectMapper().createObjectNode()
        for ((path, testFile) in testFiles.fields()) {
          convertedTestFiles.set("${taskRoots.testFilesRoot}/$path", testFile)
        }

        set(SerializationUtils.Json.TEST_FILES, convertedTestFiles)
      }
    }

    private fun ObjectNode.to9Version() {
      convertTaskObject(this)
    }

    private fun convertTaskObject(taskObject: ObjectNode) {
      val mapper = ObjectMapper()
      val files = taskObject.remove(SerializationUtils.Json.TASK_FILES) as? ObjectNode ?: mapper.createObjectNode()
      val tests = taskObject.remove(SerializationUtils.Json.TEST_FILES)
      if (tests != null) {
        for ((path, testText) in tests.fields()) {
          if (files.has(path)) continue
          if (!testText.isTextual) continue
          val testObject = mapper.createObjectNode()
          testObject.put(NAME, path)
          testObject.put(SerializationUtils.Json.TEXT, testText.asText())
          testObject.put(SerializationUtils.Json.IS_VISIBLE, false)
          files.set(path, testObject)
        }
      }

      val additionalFiles = taskObject.remove(SerializationUtils.Json.ADDITIONAL_FILES)
      if (additionalFiles != null) {
        for ((path, fileObject) in additionalFiles.fields()) {
          if (files.has(path)) continue
          if (fileObject !is ObjectNode) continue
          fileObject.put(NAME, path)
          files.set(path, fileObject)
        }
      }
      taskObject.set(SerializationUtils.Json.FILES, files)
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
    val objectMapper = StepikConnector.getMapper(SimpleModule())
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

fun getTaskRoots(language: String?): TaskRoots? {
  if (language == null) return null
  return LANGUAGE_TASK_ROOTS[language]
}