package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROBLEM_INDEX
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin

@Suppress("unused", "UNUSED_PARAMETER", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, PROBLEM_INDEX, FEEDBACK_LINK, STATUS, FEEDBACK)
abstract class CodeforcesTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonIgnore
  override var record: Int = -1

  @JsonIgnore
  override lateinit var contentTags: List<String>

  @JsonProperty(PROBLEM_INDEX)
  private var _problemIndex: String? = null
}
