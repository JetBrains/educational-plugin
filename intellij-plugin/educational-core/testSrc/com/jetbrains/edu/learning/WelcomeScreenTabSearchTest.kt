package com.jetbrains.edu.learning

import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen
import com.jetbrains.edu.learning.stepik.hyperskill.findTabByTitle
import com.jetbrains.edu.learning.stepik.hyperskill.leftPanel
import com.jetbrains.edu.learning.stepik.hyperskill.root
import com.jetbrains.edu.learning.stepik.hyperskill.title
import kotlin.test.assertNotNull as kAssertNotNull
import kotlin.test.Test

class WelcomeScreenTabSearchTest : EduTestCase() {
  @Test
  fun `test project welcome screen tab is attainable from tabbed welcome screen`() {
    val welcomeScreen = TabbedWelcomeScreen(true)
    val leftPanel = welcomeScreen.leftPanel
    kAssertNotNull(leftPanel)

    val root = leftPanel.root
    kAssertNotNull(root)

    val projectsTab = welcomeScreen.findTabByTitle("Projects")
    kAssertNotNull(projectsTab)
    assertEquals("Projects", projectsTab.title)
  }
}