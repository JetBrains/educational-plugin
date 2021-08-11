package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.function.Supplier

abstract class DataTaskAction(actionText: Supplier<String>) : ActionWithProgressIcon(actionText), DumbAware {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return
    if (project.course !is HyperskillCourse) return
    if (EduUtils.getCurrentTask(project) !is DataTask) return

    presentation.isEnabledAndVisible = true
  }
}