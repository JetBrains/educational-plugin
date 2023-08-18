package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask

abstract class CodeforcesAction : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return

    val task = project.getCurrentTask() ?: return
    if (task !is CodeforcesTask) return

    presentation.isEnabledAndVisible = true
  }
}