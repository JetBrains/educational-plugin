package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.ui.jcef.JBCefBrowserBase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.rules.ConditionalExecutionRule
import com.jetbrains.edu.rules.MinPlatformVersion
import org.junit.Rule
import org.junit.Test

class JCEFBrowserCompatibilityTest : EduTestCase() {

  @JvmField
  @Rule
  val executionRule = ConditionalExecutionRule()

  @MinPlatformVersion("242.23726")
  @Test
  fun `test JBCefBrowserBase has disableNavigation method`() {
    assertTrue(JBCefBrowserBase::class.java.declaredMethods.any { it.name == "disableNavigation" })
  }
}