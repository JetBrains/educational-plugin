package com.jetbrains.edu.learning.submissions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.*
import java.util.*

const val SUBMISSION = "submission"

class SubmissionData() {
  @JsonProperty(SUBMISSION)
  lateinit var submission: Submission

  constructor(attemptId: Int, score: String, files: List<SolutionFile>, task: Task) : this() {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    submission = Submission(score, attemptId, files, serializedTask)
  }

  // to be used for marketplace submissions creation
  constructor(passed: Boolean, files: List<SolutionFile>, task: Task) : this() {
    val objectMapper = StepikConnector.createMapper(SimpleModule())
    val serializedTask = objectMapper.writeValueAsString(TaskData(task))

    submission = Submission(task.id, if (passed) "1" else "0", files, serializedTask)
    submission.time = Date()
    submission.id = this.hashCode()
  }
}

class Submission {
  @JsonProperty(ATTEMPT)
  var attempt: Int = 0

  @JsonProperty(REPLY)
  var reply: Reply? = null

  @JsonProperty(STEP)
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

  constructor(score: String, attemptId: Int, files: List<SolutionFile>, serializedTask: String?, feedback: String? = null) {
    reply = Reply(files, score, serializedTask, feedback)
    attempt = attemptId
  }

  constructor(taskId: Int, score: String, files: List<SolutionFile>, serializedTask: String?, feedback: String? = null) {
    reply = Reply(files, score, serializedTask, feedback)
    step = taskId
  }
}