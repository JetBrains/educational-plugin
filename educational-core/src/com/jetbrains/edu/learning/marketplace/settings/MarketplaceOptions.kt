package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionSharingPreference
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {

  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun isAvailable(): Boolean {
    // Settings should not be shown on remote development because there is currently no authentication logic implemented.
    // Instead, we use a workaround by reading the JBA UID token from the local file system (see EDU-6321)
    return !RemoteEnvHelper.isRemoteDevServer()
  }

  private val shareMySolutionsCheckBox = JBCheckBox(
    EduCoreBundle.message("marketplace.solutions.sharing.checkbox"),
    MarketplaceSettings.INSTANCE.solutionsSharing ?: false
  ).apply { isEnabled = false }

  init {
    CompletableFuture.runAsync({
      val sharingPreference = MarketplaceSubmissionsConnector.getInstance().getSharingPreference()
      MarketplaceSettings.INSTANCE.solutionsSharing = if (sharingPreference != null) {
        shareMySolutionsCheckBox.isEnabled = true
        sharingPreference.toString() == MarketplaceSolutionSharingPreference.ALWAYS.toString()
      }
      else {
        MarketplaceSettings.INSTANCE.solutionsSharing
      }
      shareMySolutionsCheckBox.isSelected = MarketplaceSettings.INSTANCE.solutionsSharing ?: false
    }, ProcessIOExecutorService.INSTANCE)
  }

  override fun getDisplayName(): String = JET_BRAINS_ACCOUNT

  override fun profileUrl(account: MarketplaceAccount): String = JET_BRAINS_ACCOUNT_PROFILE_PATH

  override fun getLogoutText(): String = ""

  override fun createLogOutListener(): HyperlinkAdapter? = null

  override fun postLoginActions() {
    super.postLoginActions()
    val openProjects = ProjectManager.getInstance().openProjects
    openProjects.forEach { if (!it.isDisposed && it.course is EduCourse) SubmissionsManager.getInstance(it).prepareSubmissionsContentWhenLoggedIn() }
  }

  override fun getAdditionalComponents(): List<JComponent> =
    if (Registry.`is`(ShareMySolutionsAction.REGISTRY_KEY, false) && MarketplaceSettings.INSTANCE.getMarketplaceAccount() != null) {
      listOf(shareMySolutionsCheckBox)
    }
    else {
      listOf()
    }

  override fun apply() {
    super.apply()
    MarketplaceSettings.INSTANCE.updateSharingPreference(shareMySolutionsCheckBox.isSelected)
  }

  override fun reset() {
    super.reset()
    shareMySolutionsCheckBox.isSelected = MarketplaceSettings.INSTANCE.solutionsSharing == true
  }

  override fun isModified(): Boolean {
    return super.isModified() || MarketplaceSettings.INSTANCE.solutionsSharing != shareMySolutionsCheckBox.isSelected
  }
}