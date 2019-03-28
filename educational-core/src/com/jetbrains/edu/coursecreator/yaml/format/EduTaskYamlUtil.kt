package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.Converter
import com.jetbrains.edu.learning.courseFormat.TaskFile

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
class EduTaskYamlMixin : TaskYamlMixin() {
  @JsonProperty("files")
  @JsonSerialize(contentConverter = Converter.None::class)
  override fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }
}