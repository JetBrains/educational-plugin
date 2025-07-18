@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning.statistics.metadata

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import org.jetbrains.annotations.TestOnly

// BACKCOMPAT: 2025.1. Drop it
@Deprecated("Use `CourseMetadataManager` instead")
@Service(Service.Level.PROJECT)
@State(name = "EntryPointManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class EntryPointManager : SimplePersistentStateComponent<EntryPointManager.State>(State()), EduTestAware {

  var entryPoint: String?
    get() = state.entryPoint
    set(value) {
      state.entryPoint = value
    }

  @TestOnly
  override fun cleanUpState() {
    entryPoint = null
  }

  companion object {
    fun getInstance(project: Project): EntryPointManager = project.service()
  }

  class State : BaseState() {
    var entryPoint: String? by string()
  }
}
