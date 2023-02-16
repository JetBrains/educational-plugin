package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.coursecreator.CCUtils.updateActionGroup
import com.jetbrains.edu.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCProjectViewActionGroup : DefaultActionGroup(EduCoreBundle.lazyMessage("group.educator.projectView.text"),
                                                    EduCoreBundle.lazyMessage("group.educator.projectView.description"), null), DumbAware {
  override fun update(e: AnActionEvent) {
    updateActionGroup(e)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
