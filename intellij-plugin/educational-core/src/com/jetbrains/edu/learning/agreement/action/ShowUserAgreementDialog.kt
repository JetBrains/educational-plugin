package com.jetbrains.edu.learning.agreement.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.agreement.UserAgreementDialog

@Suppress("ComponentNotRegistered")
class ShowUserAgreementDialog : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    UserAgreementDialog.showUserAgreementDialog(e.project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}