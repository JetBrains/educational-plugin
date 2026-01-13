package com.jetbrains.edu.learning.marketplace.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import kotlin.test.Test

class LicenseLinkSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test empty link serialization`() {
    val settings = LicenseLinkSettings()
    settings.loadStateAndCheck("""
      <EduLicenseLinkSettings>
        <option name="link" />
      </EduLicenseLinkSettings>
    """)
    assertNull(settings.link)
  }

  @Test
  fun `test link serialization`() {
    val settings = LicenseLinkSettings()
    settings.loadStateAndCheck("""
      <EduLicenseLinkSettings>
        <option name="link" value="link123" />
      </EduLicenseLinkSettings>
    """)
    assertEquals("link123", settings.link)
  }
}