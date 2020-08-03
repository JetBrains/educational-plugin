package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.messages.EduCoreBundle

class ToggleRestServicesAction : ToggleAction(EduCoreBundle.message("action.toggle.rest.services.title")) {

  override fun isSelected(e: AnActionEvent): Boolean = OAuthRestService.isRestServicesEnabled()

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    OAuthRestService.setRestServicesEnabled(state)
  }
}
