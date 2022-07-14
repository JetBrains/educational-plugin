package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils.isGoIde
import com.intellij.util.PlatformUtils.isIdeaUltimate
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import javax.swing.Icon

class GoCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? =
    if (isIdeaUltimate() || isGoIde()) listOf(PluginInfo.GO) else null

  override val technologyName: String get() = "Go"
  override val logo: Icon get() = EducationalCoreIcons.GoLogo
}
