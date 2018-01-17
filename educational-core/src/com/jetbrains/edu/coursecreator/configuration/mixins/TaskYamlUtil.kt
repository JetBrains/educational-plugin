@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.TaskFile

@Suppress("UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("type")
abstract class TaskYamlMixin {
  @JsonProperty("type")
  fun getTaskType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty("task_files")
  @JsonSerialize(contentConverter = TaskFileConverter::class)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty("task_files")
  @JsonDeserialize(contentConverter = NameToTaskFile::class)
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
  }
}

private class TaskFileConverter : StdConverter<TaskFile, TaskFileWithoutPlaceholders>() {
  override fun convert(value: TaskFile): TaskFileWithoutPlaceholders {
    return TaskFileWithoutPlaceholders(value.name)
  }
}

private class TaskFileWithoutPlaceholders(@JsonProperty("name") val name: String)

private class NameToTaskFile: StdConverter<TaskFileWithoutPlaceholders, TaskFile>() {
  override fun convert(value: TaskFileWithoutPlaceholders): TaskFile {
    val taskFile = TaskFile()
    taskFile.name = value.name
    return taskFile
  }
}