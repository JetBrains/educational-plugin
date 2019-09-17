package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin.Companion.RECORD

private const val CODE = "code"
private const val SECONDS_FROM_CHANGE = "seconds_from_change"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TaskYamlMixin.TYPE, TaskYamlMixin.CUSTOM_NAME, TaskYamlMixin.FILES, TaskYamlMixin.FEEDBACK_LINK,
                   StudentTaskYamlMixin.STATUS, RECORD,
                   CODE, SECONDS_FROM_CHANGE)
class CheckiOMissionMixin : StudentTaskYamlMixin() {

  @JsonProperty(CODE)
  lateinit var myCode: String

  @JsonProperty(SECONDS_FROM_CHANGE)
  private var mySecondsFromLastChangeOnServer: Long = 0
}
