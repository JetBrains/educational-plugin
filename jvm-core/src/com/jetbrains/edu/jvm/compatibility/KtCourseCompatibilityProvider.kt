package com.jetbrains.edu.jvm.compatibility

import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.plugins.PluginInfo

class KtCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    return listOf(
      PluginInfo.KOTLIN,
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }
}
