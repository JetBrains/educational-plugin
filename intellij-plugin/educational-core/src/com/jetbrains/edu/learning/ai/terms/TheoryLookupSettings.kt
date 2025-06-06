package com.jetbrains.edu.learning.ai.terms

import com.intellij.openapi.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@State(name = "TheoryLookupSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class TheoryLookupSettings : PersistentStateComponent<TheoryLookupSettings.State> {
  private val _theoryLookupProperties = MutableStateFlow<TheoryLookupProperties?>(null)
  val theoryLookupProperties: StateFlow<TheoryLookupProperties?> = _theoryLookupProperties.asStateFlow()

  val isTheoryLookupEnabled: Boolean
    get() = _theoryLookupProperties.value?.isEnabled != false

  fun setTheoryLookupProperties(properties: TheoryLookupProperties) {
    _theoryLookupProperties.value = properties
  }

  override fun getState(): State {
    val state = State()
    state.isEnabled = isTheoryLookupEnabled
    return state
  }

  override fun loadState(state: State) {
    _theoryLookupProperties.value = TheoryLookupProperties(state.isEnabled)
  }

  // TODO(Use TheoryLookupProperties instead)
  class State : BaseState() {
    var isEnabled by property(true)
  }

  companion object {
    fun getInstance(): TheoryLookupSettings = service()
  }
}