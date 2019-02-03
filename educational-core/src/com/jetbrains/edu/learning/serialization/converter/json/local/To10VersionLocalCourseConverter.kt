package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*
import com.jetbrains.edu.learning.stepik.StepikNames

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
    localCourse.set(ADDITIONAL_FILES, additionalFiles)
    if (additionalMaterialsLesson != null) {
      localCourse.get(ITEMS).removeAll { isAdditional(it.get(TITLE).asText()) }
    }
  }

  private fun isAdditional(name: String) = (name == EduNames.ADDITIONAL_MATERIALS || name == StepikNames.PYCHARM_ADDITIONAL)

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
