package com.jetbrains.edu.ai.terms.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import com.jetbrains.edu.learning.ai.terms.TheoryLookupSettings
import org.junit.Test

class TheoryLookupSettingsTest : EduSettingsServiceTestBase() {
  @Test
  fun `test serialization`() {
    val settings = TheoryLookupSettings.getInstance()
    settings.loadStateAndCheck(
    """
      <State>
        <option name="enabled" value="false" />
      </State>
    """)
  }
}