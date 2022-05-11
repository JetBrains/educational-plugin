package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_POSSIBLE_ANSWER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.INITIAL_STATE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.INIT_FROM_DEPENDENCY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LENGTH
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.OFFSET
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.POSSIBLE_ANSWER
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SELECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STUDENT_ANSWER

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonDeserialize(builder = EduAnswerPlaceholderBuilder::class)
@JsonPropertyOrder(OFFSET, LENGTH, PLACEHOLDER_TEXT, DEPENDENCY, INITIAL_STATE, INIT_FROM_DEPENDENCY, POSSIBLE_ANSWER, SELECTED, STATUS,
                   STUDENT_ANSWER, LENGTH, OFFSET)
abstract class StudentEncryptedAnswerPlaceholderYamlMixin : StudentAnswerPlaceholderYamlMixin() {

  @JsonProperty(ENCRYPTED_POSSIBLE_ANSWER)
  @Encrypt
  private var possibleAnswer = ""

}