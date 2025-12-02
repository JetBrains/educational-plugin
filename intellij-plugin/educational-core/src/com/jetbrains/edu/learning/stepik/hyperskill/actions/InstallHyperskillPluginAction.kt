package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.installAndEnableHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.needInstallHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.restartIde

class InstallHyperskillPluginAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    runWithModalProgressBlocking(ModalTaskOwner.guess(), EduCoreBundle.message("action.Educational.Hyperskill.InstallHyperskillPlugin.progress.text")) {
      installAndEnableHyperskillPlugin()
    }
    restartIde(withConfirmationDialog = true)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = needInstallHyperskillPlugin()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    const val ACTION_ID: String = "Educational.Hyperskill.InstallHyperskillPlugin"
  }
}