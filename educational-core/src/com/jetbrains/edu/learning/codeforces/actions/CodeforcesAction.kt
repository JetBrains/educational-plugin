package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask

abstract class CodeforcesAction : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return

    val task = EduUtils.getCurrentTask(project) ?: return
    if (task !is CodeforcesTask) return

    presentation.isEnabledAndVisible = true
  }
}