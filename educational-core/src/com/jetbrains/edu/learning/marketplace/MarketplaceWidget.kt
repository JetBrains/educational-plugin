package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.LoginWidget
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.update.SyncMarketplaceCourseAction
import com.jetbrains.edu.learning.messages.EduCoreBundle

class MarketplaceWidget(project: Project) : LoginWidget<MarketplaceAccount>(project,
                                                                            EduCoreBundle.message("marketplace.widget.title"),
                                                                            EduCoreBundle.message("marketplace.widget.tooltip"),
                                                                            EducationalCoreIcons.MARKETPLACE) {

  override val account: MarketplaceAccount?
    get() = MarketplaceSettings.INSTANCE.account

  override val synchronizeCourseActionId: String
    get() = SyncMarketplaceCourseAction.ACTION_ID

  override val platformName: String
    get() = MARKETPLACE

  override fun profileUrl(account: MarketplaceAccount): String = MARKETPLACE_PROFILE_PATH

  override fun ID() = ID

  override fun authorize() {
    MarketplaceConnector.getInstance().doAuthorize()
  }

  override fun resetAccount() {
    MarketplaceSettings.INSTANCE.account = null
  }

  override fun loginNeeded(): Boolean = false

  companion object {
    const val ID = "MarketplaceWidget"
  }
}