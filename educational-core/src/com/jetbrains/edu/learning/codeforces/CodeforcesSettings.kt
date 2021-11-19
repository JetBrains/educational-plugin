package com.jetbrains.edu.learning.codeforces

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.authUtils.deserializeAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesUserInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jdom.Element
import javax.swing.Icon

private const val serviceName = "CodeforcesSettings"

@State(name = serviceName, storages = [Storage("other.xml")])
class CodeforcesSettings : PersistentStateComponent<Element> {

  @get:Transient
  @set:Transient
  var account: CodeforcesAccount? = null
    set(value) {
      if (value == null) {
        if (Messages.showOkCancelDialog(
            EduCoreBundle.message("dialog.message.are.you.sure"),
            EduCoreBundle.message("dialog.title.confirm.logout"),
            Messages.getOkButton(),
            Messages.getCancelButton(),
            AllIcons.General.QuestionDialog) == Messages.OK) {
          field = value
          ApplicationManager.getApplication().messageBus.syncPublisher(AUTHENTICATION_TOPIC).userLoggedOut()
        }
      }
      else {
        field = value
        ApplicationManager.getApplication().messageBus.syncPublisher(AUTHENTICATION_TOPIC).userLoggedIn()
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
    val accountClass = CodeforcesAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    account = deserializeAccount(user, accountClass, CodeforcesUserInfo::class.java)
  }

  fun isLoggedIn(): Boolean = account != null
  fun getLoginIcon(): Icon = if (isLoggedIn()) EducationalCoreIcons.LOGGED_IN_USER else AllIcons.General.User

  companion object {
    @JvmStatic
    fun getInstance(): CodeforcesSettings = service()
    val AUTHENTICATION_TOPIC = Topic.create("Codeforces.Authentication", EduLogInListener::class.java)
  }
}