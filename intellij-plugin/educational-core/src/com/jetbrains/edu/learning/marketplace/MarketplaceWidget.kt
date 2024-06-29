package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.update.SyncMarketplaceCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.ui.EduHyperlinkLabel
import javax.swing.JPanel

class MarketplaceWidget(project: Project) : LoginWidget<MarketplaceAccount>(
  project,
  EduCoreBundle.message("marketplace.widget.title"),
  EduCoreBundle.message("marketplace.widget.tooltip"),
  EducationalCoreIcons.Actions.EduCourse
) {
  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override val synchronizeCourseActionId: String
    get() = SyncMarketplaceCourseAction.ACTION_ID

  override fun profileUrl(account: MarketplaceAccount): String = JET_BRAINS_ACCOUNT_PROFILE_PATH

  override fun ID() = ID

  override fun loginNeeded(): Boolean = false

  override fun postLoginActions() = SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn()

  override fun addLogoutLabel(wrapperPanel: JPanel, popup: JBPopup): EduHyperlinkLabel? = null

  companion object {
    const val ID = "MarketplaceWidget"
  }
}