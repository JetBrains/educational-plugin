package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

@Service(Service.Level.PROJECT)
@State(name = "LicenseLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class LicenseLinkSettings :
  SimplePersistentStateComponent<LicenseLinkSettings.EduLicenseLinkSettings>(EduLicenseLinkSettings()),
  EduTestAware
{
  var link: String?
    get() = state.link
    set(value) {
      state.link = value
    }

  override fun cleanUpState() {
    link = null
  }

  class EduLicenseLinkSettings : BaseState() {
    var link by string()
  }

  companion object {
    fun getInstance(project: Project): LicenseLinkSettings = project.service<LicenseLinkSettings>()
  }
}
