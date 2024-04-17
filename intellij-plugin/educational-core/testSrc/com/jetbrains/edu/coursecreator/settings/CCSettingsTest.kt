package com.jetbrains.edu.coursecreator.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class CCSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = CCSettings()
    settings.loadStateAndCheck("""
      <State>
        <option name="copyTestsInFrameworkLessons" value="true" />
        <option name="isHtmlDefault" value="true" />
        <option name="showSplitEditor" value="true" />
      </State>
    """)
  }
}
