package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FILES
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin.Companion.RECORD
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin.Companion.STATUS

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TaskYamlMixin.TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, RECORD)
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

