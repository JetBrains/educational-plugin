package com.jetbrains.edu.aiHints.core.action.internal

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.aiHints.core.ui.TextHintInlineBanner
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.educational.ml.hints.hint.TextHint

@Suppress("ComponentNotRegistered")
class ShowTextHintExample : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.isEduProject() == true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    TextHintInlineBanner(project, task, EduAIHintsCoreBundle.message("action.Educational.Hints.ShowTextHintExample.message"))
      .addFeedbackLikenessButtons(task, "studentSolution", TextHint("Text Hint"))
      .display()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}