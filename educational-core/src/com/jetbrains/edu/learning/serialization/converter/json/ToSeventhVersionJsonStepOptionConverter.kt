package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonObject
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS

class ToSeventhVersionJsonStepOptionConverter(private val language: String?) : JsonStepOptionsConverter {

  override fun convert(stepOptionsJson: JsonObject): JsonObject {
    if (language == null) return stepOptionsJson
    if (stepOptionsJson.getAsJsonPrimitive(TITLE)?.asString == EduNames.ADDITIONAL_MATERIALS) return stepOptionsJson
    val (taskFilesRoot, testFilesRoot) = LANGUAGE_TASK_ROOTS[language] ?: return stepOptionsJson

    val taskFiles = stepOptionsJson.getAsJsonArray(FILES)
    if (taskFiles != null) {
      for (taskFile in taskFiles) {
        if (taskFile !is JsonObject) continue
        val path = taskFile.getAsJsonPrimitive(NAME)?.asString ?: continue
        taskFile.addProperty(NAME, "$taskFilesRoot/$path")
        for (placeholder in taskFile.getAsJsonArray(PLACEHOLDERS)) {
          val placeholderObject = placeholder as? JsonObject ?: continue
          convertPlaceholder(placeholderObject, taskFilesRoot)
        }
      }
    }

    val testFiles = stepOptionsJson.getAsJsonArray(TESTS)
    if (testFiles != null) {
      for (testFile in testFiles) {
        if (testFile !is JsonObject) continue
        val path = testFile.getAsJsonPrimitive(NAME)?.asString ?: continue
        testFile.addProperty(NAME, "$testFilesRoot/$path")
      }
    }

    return stepOptionsJson
  }

  companion object {

    @JvmStatic
    fun convertPlaceholder(placeholder: JsonObject, taskFilesRoot: String) {
      val dependency = placeholder.getAsJsonObject(DEPENDENCY) ?: return
      val dependencyFilePath = dependency.getAsJsonPrimitive(DEPENDENCY_FILE)?.asString ?: return
      dependency.addProperty(DEPENDENCY_FILE, "$taskFilesRoot/$dependencyFilePath")
    }
  }
}
