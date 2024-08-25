package com.jetbrains.edu

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.learning.EduNames
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PluginLoadingTest : BasePlatformTestCase() {

  @Test
  fun `plugin loading`() {
    val plugin = PluginManagerCore.getPlugin(PluginId.getId(EduNames.PLUGIN_ID))
    assertNotNull("JetBrains Academy plugin (${EduNames.PLUGIN_ID}) is not loaded", plugin)
  }
}
