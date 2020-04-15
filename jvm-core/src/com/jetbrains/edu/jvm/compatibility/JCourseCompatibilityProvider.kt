package com.jetbrains.edu.jvm.compatibility

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.plugins.PluginInfo

class JCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    if (EduUtils.isAndroidStudio()) return null
    return listOf(
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }
}
