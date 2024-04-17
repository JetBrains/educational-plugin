package com.jetbrains.edu.javascript.learning.checkio

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class JsCheckiOSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = JsCheckiOSettings()
    settings.loadStateAndCheck("""
      <JsCheckiOSettings>
        <CheckiOAccount>
          <option name="tokenExpiresIn" value="1698710755" />
          <option name="guest" value="false" />
          <option name="uid" value="12345678" />
          <option name="username" value="abcd" />
        </CheckiOAccount>
      </JsCheckiOSettings>
    """)
  }
}
