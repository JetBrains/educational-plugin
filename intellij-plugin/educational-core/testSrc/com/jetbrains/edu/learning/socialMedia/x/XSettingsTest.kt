package com.jetbrains.edu.learning.socialMedia.x

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class XSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state`() {
    val settings = XSettings()
    settings.loadStateAndCheck("<XSettingsState />")
    assertNull(settings.account)
  }

  @Test
  fun `test serialization with old data`() {
    val settings = XSettings()
    settings.loadStateAndCheck("""
      <XSettingsState>
        <option name="askToPost" value="true" />
        <option name="userId" value="username" />
      </XSettingsState>
    """)

    assertTrue(settings.askToPost)
    assertEquals("username", settings.userId)
    assertNull(settings.account)
  }

  @Test
  fun `test serialization with full account data`() {
    val settings = XSettings()
    settings.loadStateAndCheck("""
      <XSettingsState>
        <option name="askToPost" value="true" />
        <option name="expiresIn" value="12345" />
        <option name="name" value="name" />
        <option name="userId" value="username" />
      </XSettingsState>
    """)

    assertTrue(settings.askToPost)

    val actualAccount = kotlin.test.assertNotNull(settings.account)
    assertEquals("username", actualAccount.userInfo.userName)
    assertEquals("name", actualAccount.userInfo.name)
    assertEquals(12345, actualAccount.tokenExpiresIn)
  }
}
