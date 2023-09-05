package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CODE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SECONDS_FROM_CHANGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin

@Suppress("UNUSED_PARAMETER", "unused", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, STATUS, FEEDBACK, CODE, SECONDS_FROM_CHANGE)
abstract class CheckiOMissionYamlMixin : StudentTaskYamlMixin() {
  @JsonIgnore
  override lateinit var feedbackLink: String

  @JsonIgnore
  override var record: Int = -1

  @JsonIgnore
  override lateinit var contentTags: List<String>

  @JsonProperty(CODE)
  private lateinit var code: String

  @JsonProperty(SECONDS_FROM_CHANGE)
  private var secondsFromLastChangeOnServer: Long = 0
}
