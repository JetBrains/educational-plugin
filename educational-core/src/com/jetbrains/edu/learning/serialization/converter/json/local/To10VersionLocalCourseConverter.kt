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
    val additionalMaterialsLesson = localCourse.getJsonObjectList(ITEMS).single {
      it.get(TITLE).asText() == EduNames.ADDITIONAL_MATERIALS || it.get(TITLE).asText() == StepikNames.PYCHARM_ADDITIONAL
    }

    val task = additionalMaterialsLesson.getJsonObjectList(TASK_LIST).single {
      it.get(NAME).asText() == EduNames.ADDITIONAL_MATERIALS || it.get(TITLE).asText() == StepikNames.PYCHARM_ADDITIONAL
    }

    task.get(FILES)?.fields()?.forEach { (_, fileObject) -> additionalFiles.add(fileObject) }
    localCourse.set(ADDITIONAL_FILES, additionalFiles)
  }

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
