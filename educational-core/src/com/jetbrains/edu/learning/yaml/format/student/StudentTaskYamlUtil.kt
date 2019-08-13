package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.FILES
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin.Companion.TYPE

private const val STATUS = "status"
private const val RECORD = "record"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK,
                   STATUS, RECORD)
class StudentTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(RECORD)
  private var myRecord: Int = -1
}

