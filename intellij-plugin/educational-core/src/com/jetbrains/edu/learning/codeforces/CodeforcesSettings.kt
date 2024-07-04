package com.jetbrains.edu.learning.codeforces

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.authUtils.deserializeAccount
import com.jetbrains.edu.learning.authUtils.serialize
import com.jetbrains.edu.learning.codeforces.authorization.CodeforcesAccount
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesUserInfo
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
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
          updateCheckPanel()
          ApplicationManager.getApplication().messageBus.syncPublisher(AUTHENTICATION_TOPIC).userLoggedOut()
        }
      }
      else {
        field = value
        updateCheckPanel()
        ApplicationManager.getApplication().messageBus.syncPublisher(AUTHENTICATION_TOPIC).userLoggedIn()
      }
    }

  private fun updateCheckPanel() {
    ProjectManager.getInstance().openProjects
      .filter { !it.isDisposed }
      .forEach { project ->
        if (project.course is CodeforcesCourse) {
          val task = project.getCurrentTask()
          if (task != null) {
            project.invokeLater { TaskToolWindowView.getInstance(project).updateCheckPanel(task) }
          }
        }
      }
  }

  fun setAccountWithStatisticsEvent(account: CodeforcesAccount?, authorizationPlace: AuthorizationPlace = AuthorizationPlace.UNKNOWN) {
    this.account = account
    if (account != null) EduCounterUsageCollector.logInSucceed(YamlMixinNames.CODEFORCES_TYPE_YAML, authorizationPlace)
    else EduCounterUsageCollector.logOutSucceed(YamlMixinNames.CODEFORCES_TYPE_YAML, authorizationPlace)
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
    account = user.deserializeAccount(accountClass, CodeforcesUserInfo::class.java)
  }

  fun isLoggedIn(): Boolean = account != null
  fun getLoginIcon(): Icon = if (isLoggedIn()) EducationalCoreIcons.LOGGED_IN_USER else AllIcons.General.User

  companion object {
    fun getInstance(): CodeforcesSettings = service()
    val AUTHENTICATION_TOPIC = Topic.create("Codeforces.Authentication", EduLogInListener::class.java)
  }
}