package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SHELL
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

class ShellCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfos.SHELL)

  override val technologyName: String get() = SHELL
  override val logo: Icon get() = EducationalCoreIcons.ShellLogo
}