package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeAccount
import org.jdom.Element

private const val serviceName = "HyperskillSettings"
@State(name = serviceName, storages = [Storage("other.xml")])
class HyperskillSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  var account: HyperskillAccount? = null

  override fun getState(): Element? {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serialize() ?: return null
    mainElement.addContent(userElement)
    return mainElement
  }

  override fun loadState(settings: Element) {
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = HyperskillAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = deserializeAccount(user, accountClass, HyperskillUserInfo::class.java)
  }

  companion object {
    val INSTANCE: HyperskillSettings
      get() = ServiceManager.getService(HyperskillSettings::class.java)
  }
}
