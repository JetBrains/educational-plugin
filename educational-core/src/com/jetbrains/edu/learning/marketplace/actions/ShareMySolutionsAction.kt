package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class ShareMySolutionsAction : DumbAwareToggleAction(EduCoreBundle.message("marketplace.solutions.sharing.action")) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    super.update(e)

    e.presentation.isVisible = Registry.`is`(REGISTRY_KEY, false) && project.isMarketplaceCourse() && project.isStudentProject()
    if (!e.presentation.isVisible) return

    e.presentation.isEnabled = isAvailableInSettings()
  }

  override fun isSelected(e: AnActionEvent): Boolean = MarketplaceSettings.INSTANCE.solutionsSharing ?: false

  override fun setSelected(e: AnActionEvent, state: Boolean) = MarketplaceSettings.INSTANCE.updateSharingPreference(state)

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun isAvailableInSettings(): Boolean = MarketplaceSettings.INSTANCE.solutionsSharing != null

  companion object {

    @NonNls
    const val REGISTRY_KEY = "edu.marketplace.solutions.sharing"

    @NonNls
    const val ACTION_ID = "Educational.Student.ShareMySolutions"
  }
}