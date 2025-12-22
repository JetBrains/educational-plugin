package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.agreement.solutionSharing
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import org.jetbrains.annotations.NonNls

class ShareMySolutionsAction : DumbAwareToggleAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    super.update(e)

    e.presentation.isVisible = project.isMarketplaceCourse() && project.isStudentProject()
    if (!e.presentation.isVisible) return

    e.presentation.isEnabled = MarketplaceConnector.getInstance().isLoggedIn()
  }

  override fun isSelected(e: AnActionEvent): Boolean = UserAgreementSettings.getInstance().solutionSharing

  override fun setSelected(e: AnActionEvent, state: Boolean) = UserAgreementSettings.getInstance().setSolutionSharing()

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {

    @NonNls
    const val ACTION_ID = "Educational.Student.ShareMySolutions"
  }
}