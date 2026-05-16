package com.jetbrains.edu.learning

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Property
import org.jdom.Element

@State(name = "EduSettings", storages = [Storage("other.xml")])
class EduSettings : PersistentStateComponent<Element> {
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
    val mainElement = Element(SETTINGS_NAME)
    XmlSerializer.serializeInto(this, mainElement)
    return mainElement
  }

  override fun loadState(state: Element) {
    XmlSerializer.deserializeInto(this, state)

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

  private fun initialJavaUiLibrary(): JavaUILibrary {
    return if (JBCefApp.isSupported()) {
      JavaUILibrary.JCEF
    }
    else {
      JavaUILibrary.SWING
    }
  }

  companion object {
    private const val SETTINGS_NAME = "EduSettings"

    fun getInstance(): EduSettings = service()
  }
}
