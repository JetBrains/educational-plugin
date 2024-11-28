package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ActionWithButtonCustomComponent
import com.jetbrains.edu.learning.actions.ApplyCodeAction.Companion.isGetHintDiff
import com.jetbrains.edu.learning.actions.EduActionUtils.closeFileEditor

class CancelHint : ActionWithButtonCustomComponent(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isGetHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.project?.closeFileEditor(e)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}