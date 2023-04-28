package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "CCSettings", storages = [Storage("other.xml")])
class CCSettings : PersistentStateComponent<CCSettings.State> {
  private var state = State()

  class State {
    var isHtmlDefault = false
    var showSplitEditor = false
    var copyTestsInFrameworkLessons = false
  }

  override fun getState(): State {
    return state
  }

  override fun loadState(state: State) {
    this.state = state
  }

  fun useHtmlAsDefaultTaskFormat(): Boolean {
    return state.isHtmlDefault
  }

  fun setUseHtmlAsDefaultTaskFormat(useHtml: Boolean) {
    state.isHtmlDefault = useHtml
  }

  fun showSplitEditor(): Boolean {
    return state.showSplitEditor
  }

  fun setShowSplitEditor(value: Boolean) {
    state.showSplitEditor = value
  }

  fun copyTestsInFrameworkLessons(): Boolean {
    return state.copyTestsInFrameworkLessons
  }

  fun setCopyTestsInFrameworkLessons(value: Boolean) {
    state.copyTestsInFrameworkLessons = value
  }

  companion object {
    @JvmStatic
    fun getInstance(): CCSettings = service()
  }
}
