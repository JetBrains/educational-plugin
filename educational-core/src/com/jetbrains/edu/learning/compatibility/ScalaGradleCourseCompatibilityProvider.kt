package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

class ScalaGradleCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!PlatformUtils.isIntelliJ()) return null
    return listOf(
      PluginInfos.SCALA,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String get() = "Scala"
  override val logo: Icon get() = EducationalCoreIcons.ScalaLogo
}
