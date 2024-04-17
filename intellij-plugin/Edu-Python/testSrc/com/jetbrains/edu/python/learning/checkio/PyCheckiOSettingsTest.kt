package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class PyCheckiOSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = PyCheckiOSettings()
    settings.loadStateAndCheck("""
      <PyCheckiOSettings>
        <CheckiOAccount>
          <option name="tokenExpiresIn" value="1698710755" />
          <option name="guest" value="false" />
          <option name="uid" value="12345678" />
          <option name="username" value="abcd" />
        </CheckiOAccount>
      </PyCheckiOSettings>
    """)
  }
}
