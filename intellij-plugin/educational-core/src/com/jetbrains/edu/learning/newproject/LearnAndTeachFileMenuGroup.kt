package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

@Suppress("ComponentNotRegistered") // educational-core.xml
class LearnAndTeachFileMenuGroup : DefaultActionGroup(), DumbAware {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}