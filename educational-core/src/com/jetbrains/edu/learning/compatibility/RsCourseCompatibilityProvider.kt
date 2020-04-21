package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.learning.plugins.PluginInfo
import icons.EducationalCoreIcons
import javax.swing.Icon

class RsCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    return listOf(
      PluginInfo.RUST,
      PluginInfo.TOML
    )
  }

  override val technologyName: String get() = "Rust"
  override val logo: Icon get() = EducationalCoreIcons.RustLogo
}
