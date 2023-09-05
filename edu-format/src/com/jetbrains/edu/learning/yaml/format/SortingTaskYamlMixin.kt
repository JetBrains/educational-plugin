package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CAPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ORDERING
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, OPTIONS, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, ORDERING, TAGS)
abstract class SortingBasedTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonProperty(OPTIONS)
  private lateinit var options: List<String>

  @JsonProperty(ORDERING)
  private lateinit var ordering: IntArray
}

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, OPTIONS, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, ORDERING, TAGS)
abstract class SortingTaskYamlMixin : SortingBasedTaskYamlMixin()

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CAPTIONS, OPTIONS, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, ORDERING, TAGS)
abstract class MatchingTaskYamlMixin : SortingBasedTaskYamlMixin() {
  @JsonProperty(CAPTIONS)
  private lateinit var captions: List<String>
}