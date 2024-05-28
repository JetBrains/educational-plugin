package com.jetbrains.edu.learning.stepik.hyperskill.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeOAuthAccount
import com.jetbrains.edu.learning.authUtils.serialize
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillUserInfo
import org.jdom.Element

private const val serviceName = "HyperskillSettings"

@State(name = serviceName, storages = [Storage("other.xml")])
class HyperskillSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  @field:Volatile
  var account: HyperskillAccount? = null
    set(account) {
      field = account
      HyperskillConnector.getInstance().apply {
        if (account != null) notifyUserLoggedIn() else notifyUserLoggedOut()
      }
    }

  var updateAutomatically: Boolean = true

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
    account = user.deserializeOAuthAccount(accountClass, HyperskillUserInfo::class.java)
  }

  companion object {
    val INSTANCE: HyperskillSettings
      get() = service()
  }
}
