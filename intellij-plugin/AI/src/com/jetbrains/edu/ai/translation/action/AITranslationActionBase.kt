package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.EduCourse

abstract class AITranslationActionBase : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  protected fun isActionUnavailable(project: Project, course: EduCourse): Boolean =
    when {
      project.isDisposed -> true
      !project.isStudentProject() -> true
      !course.isMarketplaceRemote -> true
      else -> false
    }
}