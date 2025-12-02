package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.installAndEnableHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.needInstallHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.restartIde
import com.jetbrains.edu.learning.stepik.hyperskill.wasHyperskillPluginInstalled

class InstallHyperskillPluginAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    runWithModalProgressBlocking(project, EduCoreBundle.message("action.Educational.Hyperskill.InstallHyperskillPlugin.progress.text")) {
      installAndEnableHyperskillPlugin()
    }
    EditorNotifications.updateAll()
    restartIde(withConfirmationDialog = true)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && needInstallHyperskillPlugin() && !wasHyperskillPluginInstalled
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    const val ACTION_ID: String = "Educational.Hyperskill.InstallHyperskillPlugin"
  }
}