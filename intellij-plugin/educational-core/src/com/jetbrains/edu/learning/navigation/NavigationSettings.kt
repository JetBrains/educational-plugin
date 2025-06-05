package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service(Service.Level.PROJECT)
@State(name = "NavigationSettings", storages = [Storage("navigation.xml", roamingType = RoamingType.LOCAL)])
class NavigationSettings : PersistentStateComponent<NavigationSettings.State> {
  private val _navigationSettings = MutableStateFlow<NavigationProperties?>(null)
  val navigationSettings = _navigationSettings.asStateFlow()

  override fun getState(): NavigationSettings.State? {
    val state = State()
    state.currentStudyItem = _navigationSettings.value?.currentStudyItem ?: -1
    return state
  }

  override fun loadState(state: NavigationSettings.State) {
    _navigationSettings.value = NavigationProperties(state.currentStudyItem)
  }

  fun setCurrentStudyItem(currentStudyItem: Int) {
    _navigationSettings.value = NavigationProperties(currentStudyItem)
  }

  class State : BaseState() {
    var currentStudyItem by property(-1)
  }

  companion object {
    fun getInstance(project: Project): NavigationSettings = project.service()
  }
}

data class NavigationProperties(val currentStudyItem: Int)