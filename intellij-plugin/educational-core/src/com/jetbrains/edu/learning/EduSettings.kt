package com.jetbrains.edu.learning

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authUtils.deserializeOAuthAccount
import com.jetbrains.edu.learning.authUtils.serialize
import com.jetbrains.edu.learning.stepik.StepikUser
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.jdom.Element

@State(name = "EduSettings", storages = [Storage("other.xml")])
class EduSettings : PersistentStateComponent<Element> {
  @Transient
  @Volatile
  private var _user: StepikUser? = null

  @get:Transient
  @set:Transient
  var user: StepikUser?
    get() = _user
    set(user) {
      _user = user
      if (user != null) {
        StepikConnector.getInstance().notifyUserLoggedIn()
      }
      else {
        StepikConnector.getInstance().notifyUserLoggedOut()
      }
    }

  @Property
  var javaUiLibrary: JavaUILibrary = initialJavaUiLibrary()
    private set

  @Property
  private var uiLibraryChangedByUser: Boolean = false

  fun setJavaUiLibrary(javaUiLibrary: JavaUILibrary, changedByUser: Boolean) {
    this.javaUiLibrary = javaUiLibrary
    uiLibraryChangedByUser = changedByUser
  }

  override fun getState(): Element {
    return serialize()
  }

  private fun serialize(): Element {
    val mainElement = Element(SETTINGS_NAME)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = _user?.serialize() ?: return mainElement
    val userOption = Element(OPTION)
    userOption.setAttribute(NAME, USER)
    userOption.addContent(userElement)
    mainElement.addContent(userOption)
    return mainElement
  }

  override fun loadState(state: Element) {
    deserialize(state)

    // It's supposed to handle two cases:
    // - JCEF became unavailable after the previous IDE session.
    //   For example, the previous session was in another IDE version where JCEF was available and settings were migrated from there.
    //   In this case, we want to switch to Swing to avoid runtime errors.
    // - JCEF became available after the previous IDE session, although it wasn't available before.
    //   For example, it may happen during the creation of a course image for RemDev
    //   where IDE can be launched several times without JCEF during the preparation phase.
    //   In this case, we want to use JCEF if it's available in the current run.
    //   At the same time, we want to preserve user's choice
    if (javaUiLibrary == JavaUILibrary.JCEF || !uiLibraryChangedByUser) {
      javaUiLibrary = initialJavaUiLibrary()
    }
  }

  private fun deserialize(state: Element) {
    XmlSerializer.deserializeInto(this, state)
    val user = getUser(state)
    val userXml = user?.getChild(STEPIK_USER) ?: return
    _user = userXml.deserializeOAuthAccount(StepikUser::class.java, StepikUserInfo::class.java)
  }

  private fun initialJavaUiLibrary(): JavaUILibrary {
    return if (JBCefApp.isSupported()) {
      JavaUILibrary.JCEF
    }
    else {
      JavaUILibrary.SWING
    }
  }

  private fun getUser(parent: Element): Element? {
    for (child in parent.children) {
      val attribute = child.getAttribute(NAME) ?: continue
      if (USER == attribute.value) {
        return child
      }
    }
    return null
  }

  companion object {
    private const val SETTINGS_NAME = "EduSettings"
    private const val OPTION = "option"
    private const val NAME = "name"
    private const val USER = "user"
    private const val STEPIK_USER = "StepikUser"

    fun getInstance(): EduSettings = service()
    fun isLoggedIn(): Boolean = getInstance().user != null
  }
}
