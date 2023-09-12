package com.jetbrains.edu.cpp.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

class CppCatchCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfos.CATCH)

  override val technologyName: String get() = "C/C++"
  override val logo: Icon get() = EducationalCoreIcons.CppLogo
}
