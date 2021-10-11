package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_CORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_INCORRECT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SELECTED_OPTIONS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, IS_MULTIPLE_CHOICE, OPTIONS, FEEDBACK_CORRECT, FEEDBACK_INCORRECT, FILES, FEEDBACK_LINK, OPTIONS, STATUS, FEEDBACK,
                   RECORD, SELECTED_OPTIONS, TAGS)
abstract class StudentChoiceTaskYamlMixin : ChoiceTaskYamlMixin() {
  @JsonProperty(SELECTED_OPTIONS)
  private var selectedVariants = mutableListOf<Int>()

  @JsonProperty(RECORD)
  private var myRecord: Int = -1
}