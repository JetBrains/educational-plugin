package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckButtonAdditionalInformationManager
import com.jetbrains.edu.learning.xmlEscaped

/**
 * Provides access to LTI settings.
 */
@Service(Service.Level.PROJECT)
@State(name="LTISettings", reloadable = true, storages = [Storage("lti.xml")])
class LTISettingsManager(private val project: Project) : SimplePersistentStateComponent<LTISettings>(LTISettings()) {

  var settings: LTISettingsDTO?
    get() {
      val launchId = state.launchId

      if (launchId.isNullOrEmpty()) {
        return null
      }

      return LTISettingsDTO(
        launchId,
        state.lmsDescription,
        state.onlineService,
        state.returnLink
      )
    }
    set(value) {
      with(state) {
        launchId = value?.launchId
        lmsDescription = value?.lmsDescription
        onlineService = value?.onlineService ?: LTIOnlineService.STANDALONE
        returnLink = value?.returnLink
      }

      settingsUpdated(value)
    }

  override fun loadState(state: LTISettings) {
    super.loadState(state)

    settingsUpdated(settings)
  }

  private fun settingsUpdated(settings: LTISettingsDTO?) {
    val additionalInformationManager = CheckButtonAdditionalInformationManager.getInstance(project)

    if (settings == null) {
      additionalInformationManager.removeInformation(LTI_ADDITIONAL_INFORMATION_KEY)
      return
    }

    val lmsDescription = settings.lmsDescription?.xmlEscaped
    val returnLink = settings.returnLink

    val message = when {
      lmsDescription.isNullOrEmpty() && !returnLink.isNullOrEmpty() -> EduCoreBundle.message("lti.check.button.hint.link", returnLink)
      lmsDescription.isNullOrEmpty() -> EduCoreBundle.message("lti.check.button.hint")
      !returnLink.isNullOrEmpty() -> EduCoreBundle.message("lti.check.button.hint.with.lms.link", returnLink, lmsDescription)
      else -> EduCoreBundle.message("lti.check.button.hint.with.lms", lmsDescription)
    }

    additionalInformationManager.setInformation(LTI_ADDITIONAL_INFORMATION_KEY, message)
  }

  companion object {
    fun getInstance(project: Project): LTISettingsManager = project.service<LTISettingsManager>()
    private const val LTI_ADDITIONAL_INFORMATION_KEY = "lti.check.button.additional.information"
  }
}