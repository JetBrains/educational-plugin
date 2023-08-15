package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.COLUMNS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ORDERING
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ROWS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SELECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, OPTIONS, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, ORDERING, TAGS)
class TableTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonProperty(ROWS)
  private lateinit var rows: List<String>

  @JsonProperty(COLUMNS)
  private lateinit var columns: List<String>

  @JsonProperty(SELECTED)
  private lateinit var selected: Array<Array<Boolean>>
}
