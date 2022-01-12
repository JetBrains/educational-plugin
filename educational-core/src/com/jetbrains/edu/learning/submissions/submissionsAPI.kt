package com.jetbrains.edu.learning.submissions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY
import com.jetbrains.edu.learning.courseFormat.AttemptBase
import com.jetbrains.edu.learning.stepik.api.*
import java.util.*

const val SUBMISSION = "submission"

class SubmissionData {
  @JsonProperty(SUBMISSION)
  lateinit var submission: Submission

  constructor()

  constructor(submission: Submission) {
    this.submission = submission
  }
}

class Submission {
  @JsonProperty(ATTEMPT)
  var attempt: Int = 0

  @JsonProperty(REPLY)
  var reply: Reply? = null

  // WRITE_ONLY because we don't need to send it
  @JsonProperty(STEP, access = WRITE_ONLY)
  var step: Int = -1

  @JsonProperty(ID)
  var id: Int? = null

  @JsonProperty(STATUS)
  var status: String? = null

  @JsonProperty(HINT)
  var hint: String? = null

  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  @JsonProperty(TIME)
  var time: Date? = null

  constructor()

  constructor(attempt: AttemptBase, reply: Reply) {
    this.attempt = attempt.id
    this.reply = reply
  }
}