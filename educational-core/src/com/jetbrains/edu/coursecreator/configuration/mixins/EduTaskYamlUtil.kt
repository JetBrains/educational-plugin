package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.Converter
import com.jetbrains.edu.learning.courseFormat.TaskFile

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
class EduTaskYamlMixin : TaskYamlMixin() {
  @JsonProperty("task_files")
  @JsonSerialize(contentConverter = Converter.None::class)
  override fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }
}