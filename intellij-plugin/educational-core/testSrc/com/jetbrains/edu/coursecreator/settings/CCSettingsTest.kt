package com.jetbrains.edu.coursecreator.settings

import com.jetbrains.edu.learning.EduSettingsServiceTestBase

class CCSettingsTest : EduSettingsServiceTestBase() {

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
