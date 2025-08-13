package com.jetbrains.edu.learning.marketplace.settings


import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST

@Service(Service.Level.PROJECT)
@State(name = "OpenOnSiteLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class OpenOnSiteLinkSettings : SimplePersistentStateComponent<EduTrackLinkSettings>(EduTrackLinkSettings()), EduTestAware {

  var link: String?
    get() = state.link
    set(value) {
      state.link = value
    }

  companion object {
    val TRUSTED_OPEN_ON_SITE_HOSTS = mapOf(
      HYPERSKILL_DEFAULT_HOST to EduNames.JBA,
      "academy.jetbrains.com" to EduNames.LEARNING_CENTER,
      "jetbrains-academy-staging-external.labs.jb.gg" to EduNames.LEARNING_CENTER,
    )

    fun getInstance(project: Project): OpenOnSiteLinkSettings = project.service<OpenOnSiteLinkSettings>()
  }
}

class EduTrackLinkSettings : BaseState() {
  var link by string()
}
