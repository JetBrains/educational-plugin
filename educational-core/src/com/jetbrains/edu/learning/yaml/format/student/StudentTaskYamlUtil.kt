package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.TYPE
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, RECORD)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(RECORD)
  protected open var myRecord: Int = -1

  companion object {
    const val STATUS = "status"
    const val RECORD = "record"
  }
}

