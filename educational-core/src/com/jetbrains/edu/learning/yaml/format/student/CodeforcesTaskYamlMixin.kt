package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.stepik.api.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused", "UNUSED_PARAMETER", "LateinitVarOverridesLateinitVar") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK)
abstract class CodeforcesTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonIgnore
  override var record: Int = -1

  @JsonIgnore
  override lateinit var contentTags: List<String>
}
