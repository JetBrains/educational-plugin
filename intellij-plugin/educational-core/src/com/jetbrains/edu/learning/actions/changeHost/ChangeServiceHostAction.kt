package com.jetbrains.edu.learning.actions.changeHost

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction

abstract class ChangeServiceHostAction<E>(private val serviceHostManager: ServiceHostManager<E>) : DumbAwareAction()
  where E : Enum<E>,
        E : ServiceHostEnum {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedHost = ChangeServiceHostDialog(serviceHostManager, e.presentation.text).showAndGetSelectedHost() ?: return
    serviceHostManager.selectedHost = selectedHost
    thisLogger().info("Host for ${serviceHostManager.name} was changed to ${selectedHost.url}")
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
