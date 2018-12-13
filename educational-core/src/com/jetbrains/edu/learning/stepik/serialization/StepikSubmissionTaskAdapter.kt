package com.jetbrains.edu.learning.stepik.serialization

import com.google.common.annotations.VisibleForTesting
import com.google.gson.*
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.SerializationUtils.STATUS
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.converter.TaskRoots
import com.jetbrains.edu.learning.serialization.converter.json.ToFifthVersionJsonStepOptionsConverter
import com.jetbrains.edu.learning.serialization.converter.json.ToSeventhVersionJsonStepOptionConverter
import com.jetbrains.edu.learning.serialization.converter.json.local.To9VersionLocalCourseConverter
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikReplyAdapter(private val language: String?) : JsonDeserializer<StepikWrappers.Reply> {

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): StepikWrappers.Reply {
    val jsonObject = json.asJsonObject
    val initialVersion = jsonObject.migrate(JSON_FORMAT_VERSION, language)

    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.fromJson<StepikWrappers.Reply>(jsonObject).apply {
      // We need to save original version of reply object
      // to correct deserialize StepikWrappers.Reply#edu_task
      this.version = initialVersion
    }
  }

  companion object {
    /**
     * Return object version before migration
     */
    @VisibleForTesting
    @JvmStatic
    fun JsonObject.migrate(maxVersion: Int, language: String?): Int {
      val initialVersion = getAsJsonPrimitive(VERSION)?.asInt ?: 1
      var version = initialVersion
      while (version < maxVersion) {
        when (version) {
          6 -> toSeventhVersion(language)
        }
        version++
      }
      addProperty(VERSION, maxVersion)
      return initialVersion
    }

    private fun JsonObject.toSeventhVersion(language: String?) {
      val taskFilesRoot = getTaskRoots(language)?.taskFilesRoot
      if (taskFilesRoot != null) {
        for (solutionFile in getAsJsonArray("solution")) {
          solutionFile.asJsonObject.changeStringProperty(NAME) { "$taskFilesRoot/$it" }
        }
      }
    }
  }
}

class StepikSubmissionTaskAdapter @JvmOverloads constructor(
  private val replyVersion: Int = JSON_FORMAT_VERSION,
  private val language: String? = null
) : JsonSerializer<Task>, JsonDeserializer<Task> {

  private val placeholderAdapter = StepikSubmissionAnswerPlaceholderAdapter(replyVersion, language)

  override fun serialize(src: Task, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .registerTypeAdapter(AnswerPlaceholder::class.java, placeholderAdapter)
      .create()
    return serializeWithTaskType(src, gson)
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Task? {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .registerTypeAdapter(AnswerPlaceholder::class.java, placeholderAdapter)
      .create()

    json.asJsonObject.migrate(replyVersion, JSON_FORMAT_VERSION, language)
    return doDeserialize(json, gson)
  }

  companion object {

    @VisibleForTesting
    @JvmStatic
    fun JsonObject.migrate(version: Int, maxVersion: Int, language: String?) {
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

    private fun JsonObject.toFifthVersion() {
      val taskTexts = getAsJsonObject(TASK_TEXTS)
      if (taskTexts != null && taskTexts.size() > 0) {
        val description = taskTexts.entrySet().firstOrNull()?.value?.asString
        addProperty(DESCRIPTION_TEXT, description)
      }
      addProperty(DESCRIPTION_FORMAT, DescriptionFormat.HTML.toString().toLowerCase())
      remove(TASK_TEXTS)
    }

    private fun JsonObject.toSeventhVersion(language: String?) {
      val taskRoots = getTaskRoots(language)
      if (taskRoots != null) {
        val taskFiles = getAsJsonObject(TASK_FILES)
        val convertedTaskFiles = JsonObject()
        for ((path, taskFile) in taskFiles.entrySet()) {
          val convertedPath = "${taskRoots.taskFilesRoot}/$path"
          taskFile.asJsonObject.addProperty(NAME, convertedPath)
          convertedTaskFiles.add(convertedPath, taskFile)
        }

        add(TASK_FILES, convertedTaskFiles)

        val testFiles = getAsJsonObject(TEST_FILES)
        val convertedTestFiles = JsonObject()
        for ((path, testFile) in testFiles.entrySet()) {
          convertedTestFiles.add("${taskRoots.testFilesRoot}/$path", testFile)
        }

        add(TEST_FILES, convertedTestFiles)
      }
    }

    private fun JsonObject.to9Version() {
      To9VersionLocalCourseConverter.convertTaskObject(this)
    }
  }
}

private class StepikSubmissionAnswerPlaceholderAdapter(
  private val replyVersion: Int,
  private val language: String?
) : JsonSerializer<AnswerPlaceholder>, JsonDeserializer<AnswerPlaceholder> {

  override fun serialize(src: AnswerPlaceholder, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create()
    val placeholderObject = gson.toJsonTree(src).asJsonObject

    placeholderObject.add(SELECTED, JsonPrimitive(src.selected))
    placeholderObject.add(STATUS, JsonPrimitive(src.status.toString()))

    return placeholderObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(jsonElement: JsonElement,
                           type: Type,
                           jsonDeserializationContext: JsonDeserializationContext): AnswerPlaceholder {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val placeholderObject = jsonElement.asJsonObject
    placeholderObject.migrate(replyVersion, language)
    val placeholder = gson.fromJson<AnswerPlaceholder>(placeholderObject)

    if (placeholderObject.has(SELECTED)) {
      placeholder.selected = placeholderObject.get(SELECTED).asBoolean
    }

    if (placeholderObject.has(STATUS)) {
      placeholder.status = CheckStatus.valueOf(placeholderObject.get(STATUS).asString)
    }

    return placeholder
  }

  companion object {
    private fun JsonObject.migrate(version: Int, language: String?) {
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

private inline fun <reified T> Gson.fromJson(element: JsonElement): T = fromJson(element, T::class.java)

private fun getTaskRoots(language: String?): TaskRoots? {
  if (language == null) return null
  return LANGUAGE_TASK_ROOTS[language]
}

// TODO: move it into SerializationUtils after conversion into kotlin
private inline fun JsonObject.changeStringProperty(propertyName: String, action: (String) -> String) {
  val value = getAsJsonPrimitive(propertyName)?.asString ?: return
  addProperty(propertyName, action(value))
}
