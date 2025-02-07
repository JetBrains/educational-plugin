package com.jetbrains.edu.ai.clippy.assistant.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.clippy.assistant.ClippyService
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIClippyCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (result.isSolved) {
      ClippyService.getInstance(project).showWithFeedback()
    }
  }
}