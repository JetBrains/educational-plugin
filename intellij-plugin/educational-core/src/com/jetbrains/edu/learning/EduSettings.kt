package com.jetbrains.edu.learning

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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

  init {
    logJCEFStatus("EduSettings.init")
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
    logJCEFStatus("EduSettings.loadState")
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

  val javaUiLibraryWithCheck: JavaUILibrary
    get() = if (javaUiLibrary === JavaUILibrary.JCEF && JBCefApp.isSupported()) {
      JavaUILibrary.JCEF
    }
    else {
      JavaUILibrary.SWING
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

  fun logJCEFStatus(blockMessage: String) {
    LOG.info(blockMessage)
    LOG.info("  JBCefApp.IS_REMOTE_ENABLED: ${isJCEFEnabledOnRemote()}")
    LOG.info("  JBCefApp.isSupported(): ${JBCefApp.isSupported()}")
    LOG.info("  EduSettings.getInstance().javaUiLibraryWithCheck: $javaUiLibraryWithCheck")
  }

  companion object {
    private const val SETTINGS_NAME = "EduSettings"
    private const val OPTION = "option"
    private const val NAME = "name"
    private const val USER = "user"
    private const val STEPIK_USER = "StepikUser"

    fun getInstance(): EduSettings = service()
    fun isLoggedIn(): Boolean = getInstance().user != null

    private val LOG = logger<EduSettings>()

    private fun isJCEFEnabledOnRemote(): Boolean {
      return runCatching {
        val field = JBCefApp::class.java.getDeclaredField("IS_REMOTE_ENABLED")
        field.isAccessible = true
        field.getBoolean(null)
      }.getOrElse { false }
    }

  }
}
