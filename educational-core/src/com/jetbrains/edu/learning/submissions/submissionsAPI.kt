package com.jetbrains.edu.learning.submissions

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import java.util.*

const val ID = "id"
const val IS_VISIBLE = "is_visible"
const val NAME = "name"
const val PLACEHOLDERS = "placeholders"
const val STATUS = "status"
const val SUBMISSION = "submission"
const val TEXT = "text"
const val TIME = "time"

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

class SolutionFile(
  @field:JsonProperty(NAME) var name: String,
  @field:JsonProperty(TEXT) var text: String,
  @field:JsonProperty(IS_VISIBLE) var isVisible: Boolean,
  @field:JsonProperty(PLACEHOLDERS) var placeholders: List<AnswerPlaceholder>? = null
) {
  constructor() : this(name = "", text = "", isVisible = true)
}