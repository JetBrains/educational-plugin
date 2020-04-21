package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.plugins.PluginInfo
import icons.EducationalCoreIcons
import javax.swing.Icon

class JCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    if (!PlatformUtils.isIntelliJ()) return null
    return listOf(
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }

  override val technologyName: String get() = "Java"
  override val logo: Icon get() = EducationalCoreIcons.JavaLogo
}
