package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeOAuthAccount
import com.jetbrains.edu.learning.courseFormat.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import org.jdom.Element

private const val serviceName = "MarketplaceSettings"

@State(name = serviceName, storages = [Storage("other.xml")])
class MarketplaceSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  @field:Volatile
  var account: MarketplaceAccount? = null
    set(account) {
      field = account
      MarketplaceConnector.getInstance().apply {
        if (account != null) notifyUserLoggedIn() else notifyUserLoggedOut()
      }
    }

  override fun getState(): Element? {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serialize() ?: return null
    mainElement.addContent(userElement)
    return mainElement
  }

  override fun loadState(settings: Element) {
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = MarketplaceAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = deserializeOAuthAccount(user, accountClass, MarketplaceUserInfo::class.java)
  }

  companion object {
    val INSTANCE: MarketplaceSettings
      get() = service()
  }
}