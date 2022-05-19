package com.jetbrains.edu.learning.stepik.course

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlDeserializationHelper
import com.jetbrains.edu.learning.yaml.YamlDeserializerBase
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unsupportedItemTypeMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames

class StepikYamlDeserializer : YamlDeserializerBase() {
  override fun deserializeLesson(mapper: ObjectMapper, configFileText: String): Lesson {
    val treeNode = mapper.readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()

    val type = YamlDeserializationHelper.asText(treeNode.get(YamlMixinNames.TYPE))
    val clazz = when (type) {
      null, Lesson().itemType, StepikLesson().itemType -> StepikLesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, EduNames.LESSON))
    }
    return mapper.treeToValue(treeNode, clazz)
  }

  override fun deserializeLessonRemoteInfo(configFileText: String): StudyItem {
    val treeNode = YamlFormatSynchronizer.REMOTE_MAPPER.readTree(configFileText)
    return YamlFormatSynchronizer.REMOTE_MAPPER.treeToValue(treeNode, StepikLesson::class.java)
  }
}