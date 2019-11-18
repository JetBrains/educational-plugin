package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.stepik.api.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused", "UNUSED_PARAMETER") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS)
class CodeforcesTaskWithFileIOYamlMixin : CodeforcesTaskYamlMixin() {
  @JsonProperty(YamlMixinNames.INPUT_FILE)
  private lateinit var inputFileName: String

  @JsonProperty(YamlMixinNames.OUTPUT_FILE)
  private lateinit var outputFileName: String
}