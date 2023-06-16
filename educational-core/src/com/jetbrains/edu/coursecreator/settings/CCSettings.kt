package com.jetbrains.edu.coursecreator.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@State(name = "CCSettings", storages = [Storage("other.xml")])
class CCSettings : SimplePersistentStateComponent<CCSettings.State>(State()) {

  var useHtmlAsDefaultTaskFormat: Boolean by state::isHtmlDefault
  var showSplitEditor: Boolean by state::showSplitEditor
  var copyTestsInFrameworkLessons: Boolean by state::copyTestsInFrameworkLessons

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
