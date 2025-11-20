package com.jetbrains.edu.socialMedia.linkedIn

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test
import java.lang.System.currentTimeMillis

class LinkedInSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test empty state`() {
    val settings = LinkedInSettings()
    settings.loadStateAndCheck("<LinkedState />")

    assertNull(settings.account)
  }

  @Test
  fun `test serialization`() {
    val settings = LinkedInSettings()

    val currentTime = currentTimeMillis() + 12345
    settings.loadStateAndCheck("""
      <LinkedState>
        <option name="askToPost" value="false" />
        <option name="expiresIn" value="$currentTime" />
        <option name="userId" value="userId" />
        <option name="userName" value="userName" />
      </LinkedState>
    """)

    assertFalse(settings.askToPost)
    val account = kotlin.test.assertNotNull(settings.account)

    assertEquals("userId", account.userInfo.id)
    assertEquals("userName", account.userInfo.name)
    // We don't keep user email in the component. Is it ok?
    assertEquals("", account.userInfo.email)
    assertEquals(currentTime, account.tokenExpiresIn)
  }

  // Do we actually want to return a null account in case of expired token and force users to relogin?
  @Test
  fun `test serialization with too old token expiration time`() {
    val settings = LinkedInSettings()

    settings.loadStateAndCheck("""
      <LinkedState>
        <option name="askToPost" value="false" />
        <option name="expiresIn" value="12345" />
        <option name="userId" value="userId" />
        <option name="userName" value="userName" />
      </LinkedState>
    """)

    assertFalse(settings.askToPost)
    assertNull(settings.account)
  }
}
