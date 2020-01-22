package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CodeforcesSettings", storages = [Storage("other.xml")])
class CodeforcesSettings : PersistentStateComponent<CodeforcesSettings> {
  var codeforcesPreferableTextLanguage: String? = null
  var codeforcesPreferableLanguage: String? = null

  override fun getState(): CodeforcesSettings? = this

  override fun loadState(state: CodeforcesSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    @JvmStatic
    fun getInstance(): CodeforcesSettings = service()
  }
}