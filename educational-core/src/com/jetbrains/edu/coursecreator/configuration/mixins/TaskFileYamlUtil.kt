package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

@Suppress("UNUSED_PARAMETER") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class TaskFileYamlMixin {
  @JsonProperty("name")
  private lateinit var myName: String

  @JsonProperty("placeholders")
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}