package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings.Companion.isJBALoggedIn
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

@Service
class UserAgreementManager(private val scope: CoroutineScope) {
  init {
    val userAgreementSettings = UserAgreementSettings.getInstance()
    scope.launch {
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.pluginAgreement }
          .runningFold<_, UserAgreementStateEvent>(UserAgreementStateEvent.Uninitialized) { acc, new ->
            when (acc) {
              is UserAgreementStateEvent.Uninitialized -> UserAgreementStateEvent.FirstState(new.pluginAgreement)
              is UserAgreementStateEvent.FirstState -> UserAgreementStateEvent.TransitionState(acc.current, new.pluginAgreement)
              is UserAgreementStateEvent.TransitionState -> UserAgreementStateEvent.TransitionState(acc.current, new.pluginAgreement)
            }
          }.collectLatest {
            if (it !is UserAgreementStateEvent.TransitionState) return@collectLatest
            /**
             * Let's avoid reloading Edu projects if the previous agreement state is [UserAgreementState.NOT_SHOWN]
             * and the new one is [UserAgreementState.ACCEPTED], which means a new user's just accepted it.
             */
            if (it.previous == UserAgreementState.NOT_SHOWN && it.current == UserAgreementState.ACCEPTED) return@collectLatest
            reloadEduProjects()
          }
      }
      launch {
        /**
         * When either [UserAgreementSettings.UserAgreementProperties.pluginAgreement]
         * or [UserAgreementSettings.UserAgreementProperties.aiServiceAgreement] changes by user, send it to remote
         */
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.pluginAgreement to it.aiServiceAgreement }.collectLatest {
          if (it.isChangedByUser) {
            submitAgreements(it.pluginAgreement, it.aiServiceAgreement)
          }
        }
      }
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.submissionsServiceAgreement }.collectLatest {
          if (it.isChangedByUser) {
            submitSubmissionsServiceAgreement(it.submissionsServiceAgreement)
            if (it.submissionsServiceAgreement != UserAgreementState.ACCEPTED) {
              // Disable Solution Sharing on remote, when user disables Submissions functionality
              submitSharingPreference(false)
            }
          }
        }
      }
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.solutionSharingPreference }.collectLatest {
          if (it.isChangedByUser) {
            val isSolutionSharingEnabled = it.solutionSharingPreference == SolutionSharingPreference.ALWAYS
            EduCounterUsageCollector.solutionSharingState(isSolutionSharingEnabled)
            submitSharingPreference(isSolutionSharingEnabled)
          }
        }
      }
    }
  }

  fun submitCurrentAgreements() {
    val userAgreementProperties = UserAgreementSettings.getInstance().userAgreementProperties.value
    submitAgreements(userAgreementProperties.pluginAgreement, userAgreementProperties.aiServiceAgreement)
  }

  private fun submitAgreements(pluginAgreement: UserAgreementState, aiAgreement: UserAgreementState) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().updateUserAgreements(pluginAgreement, aiAgreement).onError {
        LOG.error("Failed to submit user agreements to remote: $it")
        return@launch
      }
    }
  }

  private fun submitSubmissionsServiceAgreement(state: UserAgreementState) {
    if (!isJBALoggedIn()) {
      scope.launch(Dispatchers.IO) {
        MarketplaceSubmissionsConnector.getInstance().updateSubmissionsServiceAgreement(state).onError {
          LOG.error("Failed to submit Submissions Service Agreement state $state to remote: $it")
          return@launch
        }
      }
    }
  }

  private fun submitSharingPreference(state: Boolean) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().changeSharingPreference(state).onError {
        LOG.error("Failed to submit Sharing Preference state $state to remote: $it")
        return@launch
      }
    }
  }

  fun showUserAgreement(project: Project) {
    runInEdt {
      val result = UserAgreementDialog(project).showWithResult()
      UserAgreementSettings.getInstance().setAgreementState(result)
    }
  }

  private fun reloadEduProjects() {
    val projectManager = ProjectManager.getInstance()
    for (openProject in projectManager.openProjects) {
      if (openProject.isEduYamlProject() && !openProject.isDisposed) {
        projectManager.reloadProject(openProject)
      }
    }
  }

  private sealed interface UserAgreementStateEvent {
    data object Uninitialized : UserAgreementStateEvent
    data class FirstState(val current: UserAgreementState) : UserAgreementStateEvent
    data class TransitionState(val previous: UserAgreementState, val current: UserAgreementState) : UserAgreementStateEvent
  }

  companion object {
    fun getInstance(): UserAgreementManager = service()

    private val LOG: Logger = logger<UserAgreementManager>()
  }
}