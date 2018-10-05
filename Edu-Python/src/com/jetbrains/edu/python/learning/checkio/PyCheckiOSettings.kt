package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo
import com.jetbrains.edu.learning.deserializeAccount
import org.jdom.Element

private const val serviceName = "PyCheckiOSettings"
@State(name = serviceName, storages = arrayOf(Storage("other.xml")))
class PyCheckiOSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  var account: CheckiOAccount? = null

  override fun getState(): Element? {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serialize() ?: return null
    mainElement.addContent(userElement)
    return mainElement
  }

  override fun loadState(settings: Element) {
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = CheckiOAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = deserializeAccount(user, accountClass, CheckiOUserInfo::class.java)
  }

  companion object {
    @JvmStatic
    val instance: PyCheckiOSettings
      get() = ServiceManager.getService(PyCheckiOSettings::class.java)
  }
}
