package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Provides access to LTI settings.
 */
@Service(Service.Level.PROJECT)
@State(name="LTISettings", reloadable = true, storages = [Storage("lti.xml")])
class LTISettingsManager : SimplePersistentStateComponent<LTISettings>(LTISettings()) {
  companion object {
    fun getInstance(project: Project): LTISettingsManager = project.service()
  }
}