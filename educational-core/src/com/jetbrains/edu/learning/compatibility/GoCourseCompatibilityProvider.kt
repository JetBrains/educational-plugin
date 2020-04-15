package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils.isGoIde
import com.intellij.util.PlatformUtils.isIdeaUltimate
import com.jetbrains.edu.learning.plugins.PluginInfo

class GoCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? =
    if (isIdeaUltimate() || isGoIde()) listOf(PluginInfo.GO) else null
}
