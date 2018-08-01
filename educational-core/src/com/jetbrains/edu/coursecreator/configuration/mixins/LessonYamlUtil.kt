@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.configuration.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val CONTENT = "content"
@Suppress("UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var taskList: List<Task>
}

@JsonPOJOBuilder(withPrefix = "")
private class LessonBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Lesson {
    val lesson = Lesson()
    val items = content.map {
      if (it == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      TaskWithType(it)
    }
    lesson.updateTaskList(items)
    return lesson
  }
}
