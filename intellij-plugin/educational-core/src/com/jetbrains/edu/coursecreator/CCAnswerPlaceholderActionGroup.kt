package com.jetbrains.edu.coursecreator

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.coursecreator.CCUtils.updateActionGroup

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCAnswerPlaceholderActionGroup : DefaultActionGroup(), DumbAware {
  override fun update(e: AnActionEvent) {
    updateActionGroup(e)
  }
}
