package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.CourseMode.Companion.toCourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlMapperBase.MAPPER
import com.jetbrains.edu.learning.yaml.errorHandling.*
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import org.jetbrains.annotations.VisibleForTesting

abstract class YamlDeserializerBase {
  fun deserializeItem(configName: String, mapper: ObjectMapper, configFileText: String): StudyItem {
    return when (configName) {
      COURSE_CONFIG -> mapper.deserializeCourse(configFileText)
      SECTION_CONFIG -> mapper.deserializeSection(configFileText)
      LESSON_CONFIG -> mapper.deserializeLesson(configFileText)
      TASK_CONFIG -> mapper.deserializeTask(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [com.jetbrains.edu.learning.yaml.format.CourseBuilder]
   */
  @VisibleForTesting
  fun ObjectMapper.deserializeCourse(configFileText: String): Course {
    val treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()
    val courseMode = asText(treeNode.get("mode"))
    val course = treeToValue(treeNode, Course::class.java)
    course.courseMode = if (courseMode != null) CourseMode.STUDENT else CourseMode.EDUCATOR
    return course
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeSection(configFileText: String): Section {
    val jsonNode = readNode(configFileText)
    return treeToValue(jsonNode, Section::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeLesson(configFileText: String): Lesson {
    val treeNode = readNode(configFileText)
    return treeToValue(treeNode, Lesson::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String): Task {
    val treeNode = readNode(configFileText)
    return treeToValue(treeNode, Task::class.java)
  }

  private fun ObjectMapper.readNode(configFileText: String): JsonNode =
    when (val tree = readTree(configFileText)) {
      null -> JsonNodeFactory.instance.objectNode()
      is MissingNode -> JsonNodeFactory.instance.objectNode()
      else -> tree
    }

  protected fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }

  fun getCourseMode(courseConfigText: String): CourseMode? {
    val treeNode = MAPPER.readTree(courseConfigText)
    val courseModeText = asText(treeNode.get(YamlMixinNames.MODE))
    return courseModeText?.toCourseMode()
  }

}
