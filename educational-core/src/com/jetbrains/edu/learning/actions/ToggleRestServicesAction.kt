package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.messages.EduCoreBundle

class ToggleRestServicesAction : DumbAwareToggleAction(EduCoreBundle.lazyMessage("action.toggle.rest.services.title")) {

  override fun isSelected(e: AnActionEvent): Boolean = OAuthRestService.isRestServicesEnabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    OAuthRestService.isRestServicesEnabled = state
  }
}
