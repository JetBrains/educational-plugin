package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.api.EduLoginConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.update.SyncMarketplaceCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.ui.EduHyperlinkLabel
import javax.swing.JPanel

class MarketplaceWidget(project: Project) : LoginWidget<MarketplaceAccount>(project,
                                                                            EduCoreBundle.message("marketplace.widget.title"),
                                                                            EduCoreBundle.message("marketplace.widget.tooltip"),
                                                                            EducationalCoreIcons.MARKETPLACE) {
  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override val synchronizeCourseActionId: String
    get() = SyncMarketplaceCourseAction.ACTION_ID

  override fun profileUrl(account: MarketplaceAccount): String = MARKETPLACE_PROFILE_PATH

  override fun ID() = ID

  override fun loginNeeded(): Boolean = false

  override fun logOutActionLabel(wrapperPanel: JPanel, popup: JBPopup): EduHyperlinkLabel {
    return EduHyperlinkLabel("Logout from JB account with 'Manage Licences...' action", false)
  }

  companion object {
    const val ID = "MarketplaceWidget"
  }
}