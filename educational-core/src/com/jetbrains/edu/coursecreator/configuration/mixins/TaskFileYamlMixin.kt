package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

@Suppress("UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin {
  @JsonProperty("name")
  private lateinit var name: String

  @JsonProperty("placeholders")
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}

@JsonPOJOBuilder(withPrefix = "")
private class TaskFileBuilder(@JsonProperty("name") val name: String,
                              @JsonProperty("placeholders") val placeholders: List<AnswerPlaceholder>) {
  @Suppress("unused") //used for deserialization
  private fun build(): TaskFile {
    val taskFile = TaskFile()
    taskFile.name = name
    taskFile.answerPlaceholders = placeholders
    return taskFile
  }
}