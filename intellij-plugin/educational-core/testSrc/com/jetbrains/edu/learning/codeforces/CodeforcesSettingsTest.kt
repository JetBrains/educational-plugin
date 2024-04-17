package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class CodeforcesSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = CodeforcesSettings()
    settings.loadStateAndCheck("""
      <CodeforcesSettings>
        <CodeforcesAccount>
          <option name="guest" value="false" />
          <option name="handle" value="abcd" />
          <option name="sessionExpiresAt" value="1699276693624" />
        </CodeforcesAccount>
      </CodeforcesSettings>
    """)
  }
}
