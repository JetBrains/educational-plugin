package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import org.jdom.Element

@State(name = JsCheckiOSettings.SERVICE_NAME, storages = [Storage("other.xml")])
class JsCheckiOSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  var account: CheckiOAccount? = null
    set(account) {
      field = account
      JsCheckiOOAuthConnector.apply {
        if (account != null) notifyUserLoggedIn() else notifyUserLoggedOut()
      }
    }

  override fun getState(): Element? {
    return account?.serializeIntoService(SERVICE_NAME)
  }

  override fun loadState(settings: Element) {
    account = CheckiOAccount.fromElement(settings)
  }

  companion object {
    const val SERVICE_NAME = "JsCheckiOSettings"
    fun getInstance(): JsCheckiOSettings = service()
  }
}
