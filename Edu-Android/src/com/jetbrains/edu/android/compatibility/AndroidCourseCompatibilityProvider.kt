package com.jetbrains.edu.android.compatibility

import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.plugins.PluginInfo

class AndroidCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    return listOf(
      PluginInfo.ANDROID,
      PluginInfo.KOTLIN,
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }
}
