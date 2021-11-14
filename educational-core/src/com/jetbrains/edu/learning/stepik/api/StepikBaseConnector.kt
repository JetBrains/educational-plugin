package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface StepikBaseConnector {
  fun getActiveAttempt(task: Task): Result<Attempt?, String>

  fun getActiveAttemptOrPostNew(task: Task, postNew: Boolean = false): Result<Attempt, String> {
    if (!postNew) {
      val activeAttempt = getActiveAttempt(task)
      if (activeAttempt is Ok && activeAttempt.value != null) {
        return Ok(activeAttempt.value)
      }
    }
    return postAttempt(task)
  }

  fun postAttempt(task: Task): Result<Attempt, String>
}