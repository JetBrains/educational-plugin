package com.jetbrains.edu.learning.codeforces

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.authUtils.deserializeDataFormAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesUserInfo
import org.jdom.Element
import javax.swing.Icon

private const val serviceName = "CodeForcesSettings"

@State(name = serviceName, storages = [Storage("other.xml")])
class CodeforcesSettings : PersistentStateComponent<Element> {
  var preferableTaskTextLanguage: TaskTextLanguage? = null
  var preferableLanguage: String? = null
  var doNotShowLanguageDialog: Boolean = false

  @get:Transient
  @set:Transient
  var account: CodeforcesAccount? = null

  override fun getState(): Element? {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serialize() ?: return null
    mainElement.addContent(userElement)
    return mainElement
  }

  override fun loadState(settings: Element) {
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = CodeforcesAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = deserializeDataFormAccount(user, accountClass, CodeforcesUserInfo::class.java)

  }

  fun isSet(): Boolean = preferableLanguage != null && preferableTaskTextLanguage != null

  fun isLoggedIn(): Boolean = account != null
  fun getLoginIcon(): Icon = if (isLoggedIn()) EducationalCoreIcons.LOGGED_IN_USER else AllIcons.General.User

  companion object {
    @JvmStatic
    fun getInstance(): CodeforcesSettings = service()
  }
}