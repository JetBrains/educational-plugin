package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.json.migration.MigrationNames.TASK_FILES
import com.jetbrains.edu.learning.json.migration.MigrationNames.TEST_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT

class To9VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    convertTaskObject(taskObject)
  }

  companion object {

    @JvmStatic
    fun convertTaskObject(taskObject: ObjectNode) {
      val mapper = ObjectMapper()
      val files = taskObject.remove(TASK_FILES) as? ObjectNode ?: mapper.createObjectNode()
      val tests = taskObject.remove(TEST_FILES) as? ObjectNode ?: mapper.createObjectNode()
      for ((path, testText) in tests.fields()) {
        if (files.has(path)) continue
        if (!testText.isTextual) continue
        val testObject = mapper.createObjectNode()
        testObject.put(NAME, path)
        testObject.put(TEXT, testText.asText())
        testObject.put(IS_VISIBLE, false)
        files.set<JsonNode?>(path, testObject)
      }

      val additionalFiles = taskObject.remove(ADDITIONAL_FILES) as? ObjectNode ?: mapper.createObjectNode()
      for ((path, fileObject) in additionalFiles.fields()) {
        if (files.has(path)) continue
        if (fileObject !is ObjectNode) continue
        fileObject.put(NAME, path)
        files.set<JsonNode?>(path, fileObject)
      }
      taskObject.set<JsonNode?>(FILES, files)
    }
  }
}
