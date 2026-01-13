package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

@Service(Service.Level.PROJECT)
@State(name = "LicenseLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class LicenseLinkSettings :
  SerializablePersistentStateComponent<LicenseLinkSettings.EduLicenseLinkSettings>(EduLicenseLinkSettings()),
  EduTestAware
{
  var link: String?
    get() = state.link
    set(value) {
      updateState {
        it.copy(link = value)
      }
    }

  override fun cleanUpState() {
    link = null
  }

  data class EduLicenseLinkSettings(
    var link: String? = null
  )

  companion object {
    fun isLicenseRequired(project: Project): Boolean {
      return getInstance(project).link != null
    }

    fun getInstance(project: Project): LicenseLinkSettings = project.service()
  }
}
