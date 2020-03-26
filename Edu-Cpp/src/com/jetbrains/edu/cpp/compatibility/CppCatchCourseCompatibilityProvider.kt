package com.jetbrains.edu.cpp.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider

class CppCatchCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<String>? =
    if (ApplicationInfo.getInstance().build < BUILD_193) emptyList() else listOf("org.jetbrains.plugins.clion.test.catch")

  companion object {
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")!!
  }
}
