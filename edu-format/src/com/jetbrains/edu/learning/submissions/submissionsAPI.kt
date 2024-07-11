package com.jetbrains.edu.learning.submissions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TIME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.STATUS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT
import java.util.*

abstract class Submission {
  @JsonProperty(ID)
  var id: Int? = null

  @JsonProperty(TIME)
  var time: Date? = null

  @JsonProperty(STATUS)
  var status: String? = null

  abstract var taskId: Int
  abstract val solutionFiles: List<SolutionFile>?
  abstract val formatVersion: Int?
  open fun getSubmissionTexts(taskName: String): Map<String, String>? = solutionFiles?.associate { it.name to it.text }
}

@JsonPropertyOrder(NAME, PLACEHOLDERS, IS_VISIBLE, TEXT)
class SolutionFile(
  @field:JsonProperty(NAME) var name: String,
  @field:JsonProperty(TEXT) var text: String,
  @field:JsonProperty(IS_VISIBLE) var isVisible: Boolean,
  @field:JsonProperty(PLACEHOLDERS) var placeholders: List<AnswerPlaceholder>? = null
) {
  constructor() : this(name = "", text = "", isVisible = true)
}