package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(
  YamlMixinNames.TYPE,
  YamlMixinNames.CUSTOM_NAME,
  YamlMixinNames.OPTIONS,
  YamlMixinNames.FILES,
  YamlMixinNames.FEEDBACK_LINK,
  YamlMixinNames.STATUS,
  YamlMixinNames.FEEDBACK,
  YamlMixinNames.RECORD,
  YamlMixinNames.ORDERING,
  YamlMixinNames.TAGS
)
class TableTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonProperty(YamlMixinNames.ROWS)
  private lateinit var rows: List<String>

  @JsonProperty(YamlMixinNames.COLUMNS)
  private lateinit var columns: List<String>

  @JsonProperty(YamlMixinNames.SELECTED)
  private lateinit var selected: Array<Array<Boolean>>
}