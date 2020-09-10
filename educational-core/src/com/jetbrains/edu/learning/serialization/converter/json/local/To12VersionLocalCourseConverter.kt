package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.encrypt.AES256
import com.jetbrains.edu.learning.encrypt.getAesKey
import com.jetbrains.edu.learning.serialization.SerializationUtils

class To12VersionLocalCourseConverter : JsonLocalCourseConverterBase() {
  private val aesKey = getAesKey()

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    val files = taskObject.get(SerializationUtils.Json.FILES)

    for ((_, fileObject) in files.fields()) {
      if (fileObject !is ObjectNode) continue
      val text = fileObject.get(SerializationUtils.Json.TEXT).asText()
      fileObject.put(SerializationUtils.Json.TEXT, AES256.encrypt(text, aesKey))
    }
  }

  override fun convert(localCourse: ObjectNode): ObjectNode {
    convertAdditionalFiles(localCourse)
    return super.convert(localCourse)
  }

  private fun convertAdditionalFiles(localCourse: ObjectNode) : ObjectNode {
    val files = localCourse.get(SerializationUtils.Json.ADDITIONAL_FILES) ?: return localCourse

    for (fileObject in files) {
      if (fileObject !is ObjectNode) continue
      val text = fileObject.get(SerializationUtils.Json.TEXT).asText()
      fileObject.put(SerializationUtils.Json.TEXT, AES256.encrypt(text, aesKey))
    }
    return localCourse
  }
}
