package com.jetbrains.edu.jvm.compatibility

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider

class ScalaSbtCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<String>? {
    if (EduUtils.isAndroidStudio()) return null
    return listOf(
      "org.intellij.scala",
      "com.intellij.java",
      "JUnit" // Do we really need it?
    )
  }
}
