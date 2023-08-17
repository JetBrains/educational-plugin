package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@State(name = "CCSettings", storages = [Storage("other.xml")])
class CCSettings : SimplePersistentStateComponent<CCSettings.State>(State()) {

  // Don't use property delegation like `var propertyName by state::propertyName`.
  // It doesn't work because `state` may change but delegation keeps the initial state object
  var useHtmlAsDefaultTaskFormat: Boolean
    get() = state.isHtmlDefault
    set(value) {
      state.isHtmlDefault = value
    }

  var showSplitEditor: Boolean
    get() = state.showSplitEditor
    set(value) {
      state.showSplitEditor = value
    }

  var copyTestsInFrameworkLessons: Boolean
    get() = state.copyTestsInFrameworkLessons
    set(value) {
      state.copyTestsInFrameworkLessons = value
    }

  companion object {
    fun getInstance(): CCSettings = service()
  }

  class State : BaseState() {
    // Custom name is used to keep backward compatibility
    @get:OptionTag(value = "isHtmlDefault")
    var isHtmlDefault: Boolean by property(false)
    var showSplitEditor: Boolean by property(false)
    var copyTestsInFrameworkLessons: Boolean by property(false)
  }
}
