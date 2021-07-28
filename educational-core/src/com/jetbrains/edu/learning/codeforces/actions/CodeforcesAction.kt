package com.jetbrains.edu.learning.codeforces.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import java.util.function.Supplier

abstract class CodeforcesAction(actionText: Supplier<String>): DumbAwareAction(actionText) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return

    val task = EduUtils.getCurrentTask(project) ?: return
    if (task !is CodeforcesTask) return

    presentation.isEnabledAndVisible = true
  }
}