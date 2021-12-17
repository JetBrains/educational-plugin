package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.plugins.PluginInfo
import javax.swing.Icon

class PhpCourseCompatibilityProvider : CourseCompatibilityProvider {

  override val logo: Icon get() = EducationalCoreIcons.PhpLogo

  override val technologyName: String get() = "PHP"

  override fun requiredPlugins(): List<PluginInfo>? {
    return if (isFeatureEnabled(EduExperimentalFeatures.PHP_COURSES))
      listOf(PluginInfo.PHP)
    else
      null
  }
}