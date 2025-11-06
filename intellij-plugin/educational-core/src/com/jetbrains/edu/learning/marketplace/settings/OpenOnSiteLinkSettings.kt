package com.jetbrains.edu.learning.marketplace.settings


import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST
import java.util.function.Supplier

@Service(Service.Level.PROJECT)
@State(name = "OpenOnSiteLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class OpenOnSiteLinkSettings : SimplePersistentStateComponent<EduTrackLinkSettings>(EduTrackLinkSettings()), EduTestAware {

  var link: String?
    get() = state.link
    set(value) {
      state.link = value
    }

  companion object {

    /**
     * Contains a set of trusted hosts for links to the course on Task Description view.
     * Each host is associated with message of the corresponding link
     */
    val TRUSTED_OPEN_ON_SITE_HOSTS: Map<String, Supplier<String>> = mapOf(
      HYPERSKILL_DEFAULT_HOST to EduCoreBundle.lazyMessage("action.open.on.text", EduNames.JBA),
      "academy.jetbrains.com" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
      "jetbrains-academy-staging-external.labs.jb.gg" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
      "staging.academy.labs.jb.gg" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
    )

    fun getInstance(project: Project): OpenOnSiteLinkSettings = project.service<OpenOnSiteLinkSettings>()
  }
}

class EduTrackLinkSettings : BaseState() {
  var link by string()
}
