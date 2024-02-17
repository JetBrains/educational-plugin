package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import org.jetbrains.annotations.NonNls

class ShareMySolutionsAction : DumbAwareToggleAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    super.update(e)

    e.presentation.isVisible = project.isMarketplaceCourse() && project.isStudentProject()
    if (!e.presentation.isVisible) return

    val isLoggedIn = MarketplaceConnector.getInstance().isLoggedIn()
    e.presentation.isEnabled = isLoggedIn && isAvailableInSettings()
  }

  override fun isSelected(e: AnActionEvent): Boolean = MarketplaceSettings.INSTANCE.solutionsSharing ?: false

  override fun setSelected(e: AnActionEvent, state: Boolean) = MarketplaceSettings.INSTANCE.updateSharingPreference(state)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun isAvailableInSettings(): Boolean = MarketplaceSettings.INSTANCE.solutionsSharing != null

  companion object {

    @NonNls
    const val ACTION_ID = "Educational.Student.ShareMySolutions"
  }
}