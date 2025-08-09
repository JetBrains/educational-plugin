package com.jetbrains.edu.learning.marketplace.courseStorage


import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "CourseStorageLinkSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CourseStorageLinkSettings : SimplePersistentStateComponent<EduTrackLinkSettings>(EduTrackLinkSettings()) {

  var link: String?
    get() = state.link
    set(value) {
      state.link = value
    }

  var platformName: String?
    get() = state.platformName
    set(value) {
      state.platformName = value
    }

  companion object {
    fun getInstance(project: Project): CourseStorageLinkSettings = project.service<CourseStorageLinkSettings>()
  }
}

class EduTrackLinkSettings : BaseState() {
  var link by string()
  var platformName by string()
}
