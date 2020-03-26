package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils.*

class JsCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<String>? {
    return if (isIdeaUltimate() || isWebStorm() || isPyCharmPro() || isGoIde()) {
      listOf(
        "JavaScript",
        "JavaScriptDebugger",
        "NodeJS"
      )
    } else {
      null
    }
  }
}