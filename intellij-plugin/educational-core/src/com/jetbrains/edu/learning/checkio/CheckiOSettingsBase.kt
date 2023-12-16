package com.jetbrains.edu.learning.checkio

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeOAuthAccount
import com.jetbrains.edu.learning.authUtils.serialize
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.account.CheckiOUserInfo
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import org.jdom.Element

abstract class CheckiOSettingsBase : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  @field:Volatile
  var account: CheckiOAccount? = null
    set(account) {
      field = account
      checkiOOAuthConnector.apply {
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
    val accountClass = CheckiOAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = user.deserializeOAuthAccount(CheckiOAccount::class.java, CheckiOUserInfo::class.java)?.takeIf {
      // We've changed CheckiO deserialization in 2022.1 version. It causes invalid deserialization of already existed accounts,
      // so we force user to do re-login.
      it.userInfo.getFullName().isNotEmpty()
    }
  }

  protected abstract val serviceName: String
  protected abstract val checkiOOAuthConnector: CheckiOOAuthConnector
}
