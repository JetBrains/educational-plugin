package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.plugins.PluginInfo
import javax.swing.Icon

class AndroidCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!EduUtils.isAndroidStudio()) return null
    return listOf(
      PluginInfo.ANDROID,
      PluginInfo.KOTLIN,
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }

  override val technologyName: String get() = "Android"
  override val logo: Icon get() = EducationalCoreIcons.AndroidLogo
}
