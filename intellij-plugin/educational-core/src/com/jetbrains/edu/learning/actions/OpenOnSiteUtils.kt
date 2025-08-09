package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel.Companion.ACTION_PLACE

object EduOpenOnSiteUtils {
  fun isOpenOnSiteActionEnabled(project: Project): Boolean {
    val action = ActionManager.getInstance().getAction(OpenTaskOnSiteAction.ACTION_ID)
    val anActionEvent = AnActionEvent.createEvent(
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build(),
      action.templatePresentation.clone(),
      ACTION_PLACE,
      ActionUiKind.NONE,
      null
    )
    runReadAction { ActionUtil.performDumbAwareUpdate(action, anActionEvent, false) }
    return anActionEvent.presentation.isEnabled
  }
}
