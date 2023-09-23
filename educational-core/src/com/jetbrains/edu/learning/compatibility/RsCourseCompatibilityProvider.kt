package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.PlatformUtils.isCLion
import com.intellij.util.PlatformUtils.isIdeaUltimate
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

// BACKCOMPAT 2023.2
private val BUILD_232 = BuildNumber.fromString("232")!!

private val RUST_PLUGINS = listOf(
  PluginInfos.RUST,
  PluginInfos.TOML
)

class RsCourseCompatibilityProvider : CourseCompatibilityProvider {

  @Suppress("UnstableApiUsage", "DEPRECATION")
  override fun requiredPlugins(): List<PluginInfo>? {
    if (ApplicationInfo.getInstance().build < BUILD_232) return RUST_PLUGINS
    return if (isIdeaUltimate() || isCLion() || isRustRover()) RUST_PLUGINS else null
  }

  override val technologyName: String get() = "Rust"
  override val logo: Icon get() = EducationalCoreIcons.RustLogo
}
