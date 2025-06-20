package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides access to LTI settings.
 */
@Service(Service.Level.PROJECT)
@State(name="LTISettings", reloadable = true, storages = [Storage("lti.xml")])
class LTISettingsManager : SimplePersistentStateComponent<LTISettings>(LTISettings()) {

  private val _settingsFlow: MutableStateFlow<LTISettingsDTO?> = MutableStateFlow(null)

  val settingsFlow: StateFlow<LTISettingsDTO?>
    get() = _settingsFlow.asStateFlow()

  var settings: LTISettingsDTO?
    get() = settingsFlow.value
    set(value) {
      with(state) {
        launchId = value?.launchId
        lmsDescription = value?.lmsDescription
        onlineService = value?.onlineService ?: LTIOnlineService.STANDALONE
        returnLink = value?.returnLink
      }

      _settingsFlow.value = value
    }

  override fun loadState(state: LTISettings) {
    super.loadState(state)

    // update settings flow with the new value
    val launchId = state.launchId
    _settingsFlow.value = if (launchId.isNullOrEmpty()) {
      null
    }
    else {
      LTISettingsDTO(
        launchId,
        state.lmsDescription,
        state.onlineService,
        state.returnLink
      )
    }
  }

  companion object {
    fun getInstance(project: Project): LTISettingsManager = project.service()
  }
}