package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.learning.plugins.PluginInfo

class RsCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    return listOf(
      PluginInfo.RUST,
      PluginInfo.TOML
    )
  }
}
