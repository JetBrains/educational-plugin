package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.coursecreator.CCUtils.updateActionGroup

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCFrameworkLessonActionGroup : DefaultActionGroup(), DumbAware {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    updateActionGroup(e)
  }
}