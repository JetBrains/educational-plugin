package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.Converter
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = EduTaskBuilder::class)
class EduTaskYamlMixin : TaskYamlMixin() {
  @JsonProperty("task_files")
  @JsonSerialize(contentConverter = Converter.None::class)
  override fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class EduTaskBuilder(@JsonProperty("task_files") val taskFiles: List<TaskFile>) {
  @Suppress("unused") //used for deserialization
  private fun build(): EduTask {
    val eduTask = EduTask()
    eduTask.name = "<no name>"
    for (taskFile in taskFiles) {
      eduTask.taskFiles[taskFile.name] = taskFile
    }
    return eduTask
  }

}