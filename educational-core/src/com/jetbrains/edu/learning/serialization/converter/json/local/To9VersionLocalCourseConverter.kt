package com.jetbrains.edu.learning.serialization.converter.json.local

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*

class To9VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: JsonObject, language: String) {
    convertTaskObject(taskObject)
  }

  companion object {

    @JvmStatic
    fun convertTaskObject(taskObject: JsonObject) {
      val files = taskObject.getAsJsonObject(TASK_FILES)
      val tests = taskObject.remove(TEST_FILES)?.asJsonObject
      for ((path, testText) in tests?.entrySet().orEmpty()) {
        if (testText !is JsonPrimitive) continue
        val testObject = JsonObject()
        testObject.addProperty(NAME, path)
        testObject.addProperty(TEXT, testText.asString)
        testObject.addProperty(IS_VISIBLE, false)
        files.add(path, testObject)
      }

      val additionalFiles = taskObject.remove(ADDITIONAL_FILES)?.asJsonObject
      for ((path, fileObject) in additionalFiles?.entrySet().orEmpty()) {
        if (fileObject !is JsonObject) continue
        fileObject.addProperty(NAME, path)
        files.add(path, fileObject)
      }
    }
  }
}
