@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

@Suppress("UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty("tasks")
  @JsonSerialize(contentConverter = TaskConverter::class)
  private lateinit var taskList: List<Task>
}

private class TaskConverter : StdConverter<Task, String>() {
  override fun convert(task: Task): String = task.name
}

@JsonPOJOBuilder(withPrefix = "")
private class LessonBuilder(@JsonProperty("tasks") val tasks: List<String>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Lesson {
    val lesson = Lesson()
    for (task in tasks) {
      val createdTask = object : Task() {
        override fun getTaskType() = "fake"
      }
      createdTask.name = task
      lesson.addTask(createdTask)
    }
    return lesson
  }
}
