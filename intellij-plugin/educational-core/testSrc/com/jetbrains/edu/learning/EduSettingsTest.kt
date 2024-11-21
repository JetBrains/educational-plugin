package com.jetbrains.edu.learning

import com.intellij.ui.jcef.JBCefApp
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Test

class EduSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization with JCEF`() = withJCEFSupported(true) {
    val settings = EduSettings()
    settings.checkState("""
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """)
  }

  @Test
  fun `test serialization with Swing`() = withJCEFSupported(false) {
    val settings = EduSettings()
    settings.checkState("""
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """)
  }

  @Test
  fun `test set Swing explicitly settings`() = withJCEFSupported(true) {
    val settings = EduSettings()
    settings.setJavaUiLibrary(JavaUILibrary.SWING, true)
    settings.checkState("""
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """)
  }

  @Test
  fun `test switch to JCEF from Swing`() = withJCEFSupported(true) {
    val settings = EduSettings()
    settings.loadStateAndCheck("""
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """)
  }

  @Test
  fun `test switch to Swing from JCEF`() = withJCEFSupported(false) {
    val settings = EduSettings()
    settings.loadStateAndCheck("""
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """)
  }

  @Test
  fun `test preserve user choice`() = withJCEFSupported(true) {
    val settings = EduSettings()
    settings.loadStateAndCheck("""
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """)
  }

  private fun withJCEFSupported(value: Boolean, action: () -> Unit) {
    mockkStatic(JBCefApp::class) {
      every { JBCefApp.isSupported() } returns value
      action()
    }
  }
}
