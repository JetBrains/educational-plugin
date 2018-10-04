package com.jetbrains.edu.learning.stepik.alt

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "HyperskillSettings", storages = arrayOf(Storage("other.xml")))
class HyperskillSettings : PersistentStateComponent<HyperskillSettings> {
  var account: HyperskillAccount? = null

  override fun getState(): HyperskillSettings? = this

  override fun loadState(settings: HyperskillSettings) {
    XmlSerializerUtil.copyBean(settings, this)
  }

  companion object {
    val instance: HyperskillSettings
      get() = ServiceManager.getService(HyperskillSettings::class.java)
  }
}
