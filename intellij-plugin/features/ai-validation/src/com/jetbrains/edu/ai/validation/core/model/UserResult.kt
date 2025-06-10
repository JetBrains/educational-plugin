package com.jetbrains.edu.ai.validation.core.model

import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.debugger.request.TestInfoBase

data class UserResult(
  val task: Task,
  val userSolution: Map<String, String>,
  val testInfo: TestInfoBase
)
