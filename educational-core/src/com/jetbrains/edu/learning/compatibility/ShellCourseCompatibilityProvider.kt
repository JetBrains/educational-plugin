package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SHELL
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import javax.swing.Icon

class ShellCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfo.SHELL)

  override val technologyName: String get() = SHELL
  override val logo: Icon get() = EducationalCoreIcons.ShellLogo
}