package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBAccountInfoService
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedRequestNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSuccessRequestNotification
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.getJBAUserInfo
import com.jetbrains.edu.learning.marketplace.toBoolean
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.submissions.isSolutionSharingAllowed
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanners
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.APP)
class MarketplaceSettings(private val scope: CoroutineScope) {

  private var account: MarketplaceAccount? = null

  @Volatile
  var solutionsSharing: Boolean? = null
    private set

  /**
   * Do not rely on this property deciding if the submissions can be uploaded or downloaded, get actual state from the remote
   */
  @Volatile
  var userAgreementState: UserAgreementState? = null
    private set

  /**
   * Do not rely on this property deciding if statistics can be uploaded, get actual state from the remote
   */
  @Volatile
  var statisticsCollectionState: Boolean? = null
    private set

  init {
    if (!isUnitTestMode && isJBALoggedIn()) {
      scope.launch(Dispatchers.IO) {
        val submissionsConnector = MarketplaceSubmissionsConnector.getInstance()

        val remoteAgreementState = submissionsConnector.getUserAgreementState()
        userAgreementState = remoteAgreementState
        val remoteStatisticsState = submissionsConnector.getUserStatisticsAllowedState()
        statisticsCollectionState = remoteStatisticsState

        if (!remoteAgreementState.isSolutionSharingAllowed()) return@launch

        val sharingPreference = submissionsConnector.getSharingPreference()
        solutionsSharing = sharingPreference.toBoolean()
      }
    }
  }

  fun getMarketplaceAccount(): MarketplaceAccount? {
    if (!isJBALoggedIn()) {
      account = null
      return null
    }
    val currentAccount = account
    val jbaUserInfo = getJBAUserInfo()
    if (jbaUserInfo == null) {
      val accountName = account?.userInfo?.name
      LOG.error("User info is null${if (accountName != null) " for $accountName account" else ""}")
      account = null
    }
    else if (currentAccount == null || !currentAccount.checkTheSameUserAndUpdate(jbaUserInfo)) {
      account = MarketplaceAccount(jbaUserInfo)
    }

    return account
  }

  fun setAccount(value: MarketplaceAccount?) {
    account = value
  }

  fun updateSharingPreference(state: Boolean, project: Project? = null) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().changeSharingPreference(state).onError {
        SolutionSharingInlineBanners.showFailedToEnableSolutionSharing(project)
        return@launch
      }

      solutionsSharing = state
      if (state) {
        SolutionSharingInlineBanners.showSuccessSolutionSharingEnabling(project)
      }
      EduCounterUsageCollector.solutionSharingState(state)
    }
  }

  fun updateAgreementState(state: UserAgreementState, project: Project? = null) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().changeUserAgreementState(state).onError {
        showFailedRequestNotification(
          project,
          EduCoreBundle.message("user.agreement.changed.failed.notification.title"),
          EduCoreBundle.message("notification.something.went.wrong.text")
        )
        return@launch
      }

      userAgreementState = state

      val notificationText = if (state == UserAgreementState.ACCEPTED) {
        EduCoreBundle.message("user.agreement.changed.accepted.notification.text")
      }
      else {
        EduCoreBundle.message("user.agreement.changed.terminated.notification.text")
      }

      showSuccessRequestNotification(
        project,
        EduCoreBundle.message("user.agreement.changed.success.notification.title"),
        notificationText
      )
    }
  }

  @VisibleForTesting
  fun setTestAgreementState(state: UserAgreementState?) {
    userAgreementState = state
  }

  fun updateStatisticsCollectionState(state: Boolean, project: Project? = null) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().changeUserStatisticsAllowedState(state).onError {
        showFailedRequestNotification(
          project,
          EduCoreBundle.message("user.statistics.changed.failed.notification.title"),
          EduCoreBundle.message("notification.something.went.wrong.text")
        )
        return@launch
      }

      statisticsCollectionState = state
      val notificationText = if (state) {
        EduCoreBundle.message("user.statistics.changed.allowed.notification.text")
      }
      else {
        EduCoreBundle.message("user.statistics.changed.prohibited.notification.text")
      }

      showSuccessRequestNotification(
        project,
        EduCoreBundle.message("user.statistics.changed.success.notification.title"),
        notificationText
      )
    }
  }

  fun updateToActualUserAgreementState(processUpdate: suspend (UserAgreementState?) -> Unit) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      val actualAgreementState = MarketplaceSubmissionsConnector.getInstance().getUserAgreementState()
      userAgreementState = actualAgreementState
      processUpdate(actualAgreementState)
    }
  }

  fun updateToActualStatisticsSharingState(processUpdate: suspend (Boolean?) -> Unit) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      val actualStatisticsState = MarketplaceSubmissionsConnector.getInstance().getUserStatisticsAllowedState()
      statisticsCollectionState = actualStatisticsState
      processUpdate(actualStatisticsState)
    }
  }

  @TestOnly
  fun setSharingPreference(state: Boolean?) {
    solutionsSharing = state
  }

  companion object {
    private val LOG = logger<MarketplaceSettings>()

    fun isJBALoggedIn(): Boolean = JBAccountInfoService.getInstance()?.userData != null

    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}