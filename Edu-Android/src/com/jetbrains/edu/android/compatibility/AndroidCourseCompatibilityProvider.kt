package com.jetbrains.edu.android.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider

class AndroidCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<String>? {
    val plugins = mutableListOf(
      "org.jetbrains.android",
      "org.jetbrains.kotlin", // Not sure that
      "com.intellij.java",
      "org.jetbrains.plugins.gradle", // Gradle plugin, since 193 is named `Gradle-Java`
      "JUnit"
    )
    if (ApplicationInfo.getInstance().build >= BUILD_193) {
      plugins += "com.intellij.gradle"
    }
    return plugins
  }

  companion object {
    // BACKCOMPAT: 2019.3
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")!!
  }
}
