package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SUBMISSION_LANGUAGE

@Suppress("unused") // used for yaml serialization
class CodeTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(SUBMISSION_LANGUAGE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var submissionLanguage: String? = null

}
