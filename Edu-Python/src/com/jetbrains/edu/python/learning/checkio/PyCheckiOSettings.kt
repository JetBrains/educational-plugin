package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import org.jdom.Element

private const val serviceName = "PyCheckiOSettings"
@State(name = serviceName, storages = [Storage("other.xml")])
class PyCheckiOSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  @field:Volatile
  var account: CheckiOAccount? = null
    set(account) {
      field = account
      PyCheckiOOAuthConnector.apply {
        if (account != null) notifyUserLoggedIn() else notifyUserLoggedOut()
      }
    }

  override fun getState(): Element? {
    return account?.serializeIntoService(serviceName)
  }

  override fun loadState(settings: Element) {
    account = CheckiOAccount.fromElement(settings)
  }

  companion object {
    @JvmStatic
    fun getInstance(): PyCheckiOSettings = service()
  }
}
