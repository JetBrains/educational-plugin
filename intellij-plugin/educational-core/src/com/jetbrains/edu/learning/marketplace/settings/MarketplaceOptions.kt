package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {

  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun isAvailable(): Boolean {
    // Settings should not be shown on remote development because there is currently no authentication logic implemented.
    // Instead, we use a workaround by reading the JBA UID token from the local file system (see EDU-6321)
    return !RemoteEnvHelper.isRemoteDevServer()
  }

  private val shareMySolutionsCheckBox = JBCheckBox(EduCoreBundle.message("marketplace.options.solutions.sharing.checkbox")).apply {
    val sharingPreference = MarketplaceSettings.INSTANCE.solutionsSharing
    isSelected = sharingPreference ?: false
    isEnabled = sharingPreference != null && MarketplaceSettings.isJBALoggedIn()
  }

  private val userAgreementCheckBox = JBCheckBox(EduCoreBundle.message("marketplace.options.user.agreement.checkbox")).apply {
    updateUserAgreementState()
  }

  private fun JBCheckBox.updateUserAgreementState() {
    fun updateCheckbox(agreementState: UserAgreementState?, enabled: Boolean = agreementState != null) {
      isSelected = agreementState == UserAgreementState.ACCEPTED && MarketplaceSettings.isJBALoggedIn()
      isEnabled = enabled
    }

    val lastAgreementState = MarketplaceSettings.INSTANCE.userAgreementState
    updateCheckbox(lastAgreementState, false)
    MarketplaceSettings.INSTANCE.updateToActualUserAgreementState {
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        updateCheckbox(it)
      }
    }
  }

  private val statisticsCollectionAllowedCheckBox = JBCheckBox(EduCoreBundle.message("marketplace.options.statistics.checkbox")).apply {
   updateStatisticsCollectionState()
  }

  private fun JBCheckBox.updateStatisticsCollectionState() {
    fun updateCheckbox(statisticsState: Boolean?, enabled: Boolean = statisticsState != null) {
      val isStatisticsCollectionAllowed = statisticsState ?: false
      isSelected = isStatisticsCollectionAllowed && MarketplaceSettings.isJBALoggedIn()
      isEnabled = enabled
    }

    val lastStatisticsState = MarketplaceSettings.INSTANCE.statisticsCollectionState
    updateCheckbox(lastStatisticsState, false)

    MarketplaceSettings.INSTANCE.updateToActualStatisticsSharingState {
      withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
        updateCheckbox(it)
      }
    }
  }

  override fun getDisplayName(): String = JET_BRAINS_ACCOUNT

  override fun profileUrl(account: MarketplaceAccount): String = JET_BRAINS_ACCOUNT_PROFILE_PATH

  override fun getLogoutText(): String = ""

  override fun createLogOutListener(): HyperlinkAdapter? = null

  override fun postLoginActions() {
    super.postLoginActions()
    val openProjects = ProjectManager.getInstance().openProjects
    openProjects.forEach { if (!it.isDisposed && it.course is EduCourse) SubmissionsManager.getInstance(it).prepareSubmissionsContentWhenLoggedIn() }
    statisticsCollectionAllowedCheckBox.updateStatisticsCollectionState()
    userAgreementCheckBox.updateUserAgreementState()
  }

  override fun getAdditionalComponents(): List<JComponent> =
    listOf(userAgreementCheckBox, shareMySolutionsCheckBox, statisticsCollectionAllowedCheckBox)

  override fun apply() {
    super.apply()
    val settings = MarketplaceSettings.INSTANCE
    if (settings.isSolutionSharingStateModified()) {
      settings.updateSharingPreference(shareMySolutionsCheckBox.isSelected)
    }

    if (settings.isUserAgreementStateModified()) {
      val agreementState = if (userAgreementCheckBox.isSelected) {
        UserAgreementState.ACCEPTED
      }
      else {
        UserAgreementState.TERMINATED
      }
      settings.updateAgreementState(agreementState)
    }

    if (settings.isStatisticsCollectionStateModified()) {
      settings.updateStatisticsCollectionState(statisticsCollectionAllowedCheckBox.isSelected)
    }
  }

  override fun reset() {
    super.reset()
    val settings = MarketplaceSettings.INSTANCE
    shareMySolutionsCheckBox.isSelected = settings.solutionsSharing == true
    userAgreementCheckBox.isSelected = settings.userAgreementState == UserAgreementState.ACCEPTED
    statisticsCollectionAllowedCheckBox.isSelected = settings.statisticsCollectionState == true
  }

  override fun isModified(): Boolean {
    val settings = MarketplaceSettings.INSTANCE
    return super.isModified() ||
           settings.isSolutionSharingStateModified() ||
           settings.isUserAgreementStateModified() ||
           settings.isStatisticsCollectionStateModified()
  }

  private fun MarketplaceSettings.isSolutionSharingStateModified(): Boolean = solutionsSharing != shareMySolutionsCheckBox.isSelected

  private fun MarketplaceSettings.isUserAgreementStateModified(): Boolean = userAgreementState == UserAgreementState.ACCEPTED != userAgreementCheckBox.isSelected

  private fun MarketplaceSettings.isStatisticsCollectionStateModified(): Boolean = statisticsCollectionState != statisticsCollectionAllowedCheckBox.isSelected
}