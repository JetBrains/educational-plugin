package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings.Companion.isJBALoggedIn
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.submissions.SolutionSharingPreference
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

@Service
class UserAgreementManager(private val scope: CoroutineScope) {
  init {
    val userAgreementSettings = UserAgreementSettings.getInstance()
    scope.launch {
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.pluginAgreement }.collectLatest {
          if (it.isChangedByUser) {
            reloadProjectOnAgreementChange()
            updateAgreementState(it.pluginAgreement)
          }
        }
      }
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.aiServiceAgreement }.collectLatest {
          if (it.isChangedByUser) {
            MarketplaceSettings.INSTANCE.updateAiFeaturesAgreementState(it.aiServiceAgreement)
          }
        }
      }
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.submissionsServiceAgreement }.collectLatest {
          if (it.isChangedByUser) {

          }
        }
      }
      launch {
        userAgreementSettings.userAgreementProperties.distinctUntilChangedBy { it.solutionSharing }.collectLatest {
          if (it.isChangedByUser) {
            MarketplaceSettings.INSTANCE.updateSharingPreference(
              it.solutionSharing == SolutionSharingPreference.ALWAYS
            )
          }
        }
      }
    }
  }

  private fun updateAgreementState(state: UserAgreementState) {
    if (!isJBALoggedIn()) return
    scope.launch(Dispatchers.IO) {
      MarketplaceSubmissionsConnector.getInstance().changeUserAgreementState(state).onError {
        LOG.error("Failed to submit Plugin User Agreement state $state to remote: $it")
        return@launch
      }
    }
  }

  fun showUserAgreement(project: Project) {
    runInEdt {
      UserAgreementDialog.showUserAgreementDialog(project)
    }
  }

  private fun reloadProjectOnAgreementChange() {
    val projectManager = ProjectManager.getInstance()
    for (openProject in projectManager.openProjects) {
      if (openProject.isEduYamlProject() && !openProject.isDisposed) {
        projectManager.reloadProject(openProject)
      }
    }
  }

  companion object {
    fun getInstance(): UserAgreementManager = service()

    private val LOG = thisLogger()
  }
}