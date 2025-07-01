package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

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
    //TODO implement
  }

  companion object {
    fun getInstance(project: Project): LTISettingsManager = project.service<LTISettingsManager>()
  }
}