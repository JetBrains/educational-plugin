package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import javax.swing.Icon

class ScalaSbtCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!PlatformUtils.isIntelliJ()) return null
    return listOf(
      PluginInfo.SCALA,
      PluginInfo.JAVA,
      PluginInfo.JUNIT
    )
  }

  override val technologyName: String get() = "Scala"
  override val logo: Icon get() = EducationalCoreIcons.ScalaLogo
}
