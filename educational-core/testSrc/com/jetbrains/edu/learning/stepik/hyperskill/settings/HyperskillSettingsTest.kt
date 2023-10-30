package com.jetbrains.edu.learning.stepik.hyperskill.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase

class HyperskillSettingsTest : EduSettingsServiceTestBase() {

  fun `test serialization`() {
    val settings = HyperskillSettings()
    settings.loadStateAndCheck("""
      <HyperskillSettings>
        <option name="updateAutomatically" value="false" />
        <HyperskillAccount>
          <option name="tokenExpiresIn" value="1698707885" />
          <option name="email" value="abcd@example.com" />
          <option name="fullname" value="Abcd Efgh" />
          <option name="guest" value="false" />
          <option name="hyperskillProjectId" value="123" />
          <option name="id" value="12345678" />
        </HyperskillAccount>
      </HyperskillSettings>
    """)
  }
}
