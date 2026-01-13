package com.jetbrains.edu.learning.marketplace.settings


import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

@Service(Service.Level.PROJECT)
@State(name = "OpenOnSiteLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class OpenOnSiteLinkSettings : SimplePersistentStateComponent<EduTrackLinkSettings>(EduTrackLinkSettings()), EduTestAware {

  var link: String?
    get() = state.link
    set(value) {
      state.link = value
    }

  companion object {
    fun getInstance(project: Project): OpenOnSiteLinkSettings = project.service<OpenOnSiteLinkSettings>()
  }
}

class EduTrackLinkSettings : BaseState() {
  var link by string()
}
