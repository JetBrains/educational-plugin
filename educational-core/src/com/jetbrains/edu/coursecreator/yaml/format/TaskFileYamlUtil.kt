package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
abstract class TaskFileYamlMixin {
  @JsonProperty("name")
  private lateinit var myName: String

  @JsonProperty("placeholders")
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}