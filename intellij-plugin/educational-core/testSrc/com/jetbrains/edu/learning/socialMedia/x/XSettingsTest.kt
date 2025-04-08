package com.jetbrains.edu.learning.socialMedia.x

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class XSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state`() {
    val settings = XSettings()
    settings.loadStateAndCheck("<SocialMediaSettingsState />")
  }

  @Test
  fun `test serialization`() {
    val settings = XSettings()
    settings.loadStateAndCheck("""
      <SocialMediaSettingsState>
        <option name="askToPost" value="true" />
        <option name="userId" value="username" />
      </SocialMediaSettingsState>
    """)

    assertTrue(settings.askToPost)
    assertEquals("username", settings.userId)
  }
}
