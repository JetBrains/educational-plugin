package com.jetbrains.edu.aiHints.core.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIHintsCheckListener : CheckListener {
  override fun beforeCheck(project: Project, task: Task) {
    EduActionUtils.HintStateManager.getInstance(project).reset()
  }
}