package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.json.migration.MigrationNames.ADDITIONAL_MATERIALS
import com.jetbrains.edu.learning.json.migration.MigrationNames.PYCHARM_ADDITIONAL
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TITLE

class To10VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    convertAdditionalFiles(localCourse)
    return super.convert(localCourse)
  }

  private fun convertAdditionalFiles(localCourse: ObjectNode) {
    val additionalFiles = ObjectMapper().createArrayNode()
    val courseItems = localCourse.getJsonObjectList(ITEMS)
    val additionalMaterialsLesson = courseItems.singleOrNull {
      isAdditional(it.get(TITLE).asText())
    }

    val task = additionalMaterialsLesson?.getJsonObjectList(TASK_LIST)?.singleOrNull {
      isAdditional(it.get(NAME).asText())
    }

    task?.get(FILES)?.fields()?.forEach { (_, fileObject) -> additionalFiles.add(fileObject) }
    localCourse.set<JsonNode?>(ADDITIONAL_FILES, additionalFiles)
    if (additionalMaterialsLesson != null) {
      localCourse.get(ITEMS).removeAll { isAdditional(it.get(TITLE).asText()) }
    }
  }

  private fun isAdditional(name: String) = (name == ADDITIONAL_MATERIALS || name == PYCHARM_ADDITIONAL)

  override fun convertTaskObject(taskObject: ObjectNode, language: String) {
    convertTaskObject(taskObject)
  }

  companion object {
    fun convertTaskObject(taskObject: ObjectNode) {
      val descriptionFormat = taskObject.get(DESCRIPTION_FORMAT)?.asText()
      if (descriptionFormat != null) {
        taskObject.put(DESCRIPTION_FORMAT, descriptionFormat.uppercase())
      }
    }
  }
}
