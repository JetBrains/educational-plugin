package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.learning.courseFormat.PluginInfo

/**
 * Requires a plugin which is not available in any IDE.
 * It allows testing the "plugins are required to start a course" scenario regardless of the base IDE tests are run with.
 */
class FakeCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo> = listOf(FAKE_LANGUAGE_PLUGIN)

  @Suppress("HardCodedStringLiteral")
  override val technologyName: String get() = "Fake"

  companion object {
    val FAKE_LANGUAGE_PLUGIN: PluginInfo = PluginInfo("com.jetbrains.edu.fake", "Fake Langugage Plugin")
  }
}
