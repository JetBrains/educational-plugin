package com.jetbrains.edu.cpp.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.plugins.PluginInfo

class CppGTestCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? =
    if (ApplicationInfo.getInstance().build < BUILD_193) emptyList() else listOf(PluginInfo.GOOGLE_TEST)

  companion object {
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")!!
  }
}
