package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin.Companion.CUSTOM_NAME
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin.Companion.FEEDBACK_LINK
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin.Companion.FILES
import com.jetbrains.edu.coursecreator.yaml.format.TaskYamlMixin.Companion.TYPE
import com.jetbrains.edu.learning.courseFormat.CheckStatus

private const val STATUS = "status"
private const val RECORD = "record"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK,
                   STATUS, RECORD)
class EduTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(RECORD)
  private var myRecord: Int = -1
}

