package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.rust.cargo.project.settings.rustSettings

@Service(Service.Level.PROJECT)
class RsCourseProjectRefreshService(private val project: Project) {

  /**
   * If [state] is not `null` then [com.jetbrains.edu.rust.RsCourseBuilder.refreshProject] shouldn't invoke project refresh.
   * Otherwise, everything should work as expected
   */
  private var state: State? = null


  fun disableProjectRefresh() {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
    check(state == null)

    val newState = State(previousAutoUpdateState = project.rustSettings.autoUpdateEnabled)
    project.rustSettings.modify { it.autoUpdateEnabled = false }

    state = newState
  }

  fun enableProjectRefresh() {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
    val oldState = state
    check(oldState != null)

    project.rustSettings.modify { it.autoUpdateEnabled = oldState.previousAutoUpdateState }

    state = null
  }

  val isRefreshEnabled: Boolean = state == null

  companion object {
    fun getInstance(project: Project): RsCourseProjectRefreshService = project.service()
  }

  /**
   * Holds the previous user state of [org.rust.cargo.project.settings.RustProjectSettingsService.autoUpdateEnabled]
   */
  private data class State(val previousAutoUpdateState: Boolean)
}
