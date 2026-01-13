package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.marketplace.license.LicenseChecker
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import org.jetbrains.annotations.NonNls

class CheckLicenseAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    LicenseChecker.getInstance(project).scheduleLicenseCheck()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = LicenseLinkSettings.isLicenseRequired(project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Student.CheckLicenseAction"
  }
}