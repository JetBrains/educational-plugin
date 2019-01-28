package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.DESCRIPTION_FORMAT

class To10VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    convertTaskObject(taskObject)
  }

  companion object {
    @JvmStatic
    fun convertTaskObject(taskObject: ObjectNode) {
      val descriptionFormat = taskObject.get(DESCRIPTION_FORMAT)?.asText()
      if (descriptionFormat != null) {
        taskObject.put(DESCRIPTION_FORMAT, descriptionFormat.toUpperCase())
      }
    }
  }
}
