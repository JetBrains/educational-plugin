package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.submissions.SubmissionsManager

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {

  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun isAvailable(): Boolean = true

  override fun getDisplayName(): String = JET_BRAINS_ACCOUNT

  override fun profileUrl(account: MarketplaceAccount): String = JET_BRAINS_ACCOUNT_PROFILE_PATH

  override fun getLogoutText(): String = ""

  override fun createLogOutListener(): HyperlinkAdapter? = null

  override fun postLoginActions() {
    super.postLoginActions()
    val openProjects = ProjectManager.getInstance().openProjects
    openProjects.forEach {
      if (!it.isDisposed && it.course is EduCourse) SubmissionsManager.getInstance(it).prepareSubmissionsContentWhenLoggedIn()
    }
  }
}