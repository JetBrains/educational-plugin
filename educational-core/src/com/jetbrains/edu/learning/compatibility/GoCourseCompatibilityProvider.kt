package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils.isGoIde
import com.intellij.util.PlatformUtils.isIdeaUltimate

class GoCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<String>? =
    if (isIdeaUltimate() || isGoIde()) listOf("org.jetbrains.plugins.go") else null
}
