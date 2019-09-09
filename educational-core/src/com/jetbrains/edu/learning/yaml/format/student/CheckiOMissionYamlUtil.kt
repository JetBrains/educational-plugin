package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin

private const val CODE = "code"
private const val SECONDS_FROM_CHANGE = "seconds_from_change"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TaskYamlMixin.TYPE, TaskYamlMixin.CUSTOM_NAME, TaskYamlMixin.FILES,
                   StudentTaskYamlMixin.STATUS,
                   CODE, SECONDS_FROM_CHANGE)
abstract class CheckiOMissionYamlMixin : StudentTaskYamlMixin() {
  @JsonIgnore
  override lateinit var myFeedbackLink: FeedbackLink

  @JsonIgnore
  override var myRecord: Int = -1

  @JsonProperty(CODE)
  private lateinit var myCode: String

  @JsonProperty(SECONDS_FROM_CHANGE)
  private var mySecondsFromLastChangeOnServer: Long = 0
}
