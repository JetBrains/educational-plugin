package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import javax.swing.Icon

class AndroidCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!EduUtilsKt.isAndroidStudio()) return null
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
