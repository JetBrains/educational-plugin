package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils.isIdeaUltimate
import com.intellij.util.PlatformUtils.isPhpStorm
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

class PhpCourseCompatibilityProvider : CourseCompatibilityProvider {

  override val logo: Icon get() = EducationalCoreIcons.Language.PhpLogo

  override val technologyName: String get() = "PHP"

  @Suppress("UnstableApiUsage")
  override fun requiredPlugins(): List<PluginInfo>? {
    return if (isPhpStorm() || isIdeaUltimate()) listOf(PluginInfos.PHP) else null
  }
}