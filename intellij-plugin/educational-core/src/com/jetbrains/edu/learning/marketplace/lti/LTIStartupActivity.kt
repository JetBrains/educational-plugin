package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckButtonAdditionalInformationManager
import com.jetbrains.edu.learning.xmlEscaped
import kotlinx.coroutines.flow.collectLatest

class LTIStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    LTISettingsManager.instance(project).settingsFlow.collectLatest {
      updateCheckButtonAdditionalInformation(project, it)
    }
  }

  private fun updateCheckButtonAdditionalInformation(project: Project, settings: LTISettingsDTO?) {
    val additionalInformationManager = CheckButtonAdditionalInformationManager.instance(project)

    if (settings == null) {
      additionalInformationManager.removeInformation(LTI_ADDITIONAL_INFORMATION_KEY)
      return
    }

    val lmsDescription = settings.lmsDescription

    val message = if (lmsDescription.isNullOrEmpty()) {
      EduCoreBundle.message("lti.check.button.hint")
    }
    else {
      EduCoreBundle.message("lti.check.button.hint.with.lms", lmsDescription.xmlEscaped)
    }

    additionalInformationManager.setInformation(LTI_ADDITIONAL_INFORMATION_KEY, message)
  }

  companion object {
    private const val LTI_ADDITIONAL_INFORMATION_KEY = "lti.check.button.additional.information"
  }
}