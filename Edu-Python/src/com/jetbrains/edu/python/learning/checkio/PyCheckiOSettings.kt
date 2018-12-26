package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo
import org.jdom.Element

private const val serviceName = "PyCheckiOSettings"
@State(name = serviceName, storages = arrayOf(Storage("other.xml")))
class PyCheckiOSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  var account: CheckiOAccount? = null

  override fun getState(): Element? {
    return account?.serializeIntoService(serviceName)
  }

  override fun loadState(settings: Element) {
    account = CheckiOAccount.fromElement(settings)
  }

  companion object {
    @JvmField val INSTANCE: PyCheckiOSettings = ServiceManager.getService(PyCheckiOSettings::class.java)
  }
}
