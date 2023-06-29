package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.isFeatureEnabled

class SqlGradleCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)) return null
    @Suppress("UnstableApiUsage")
    if (!PlatformUtils.isCommercialEdition()) return null
    return listOf(
      PluginInfo.SQL,
      PluginInfo.JAVA,
      PluginInfo.GRADLE,
      PluginInfo.JUNIT
    )
  }

  override val technologyName: String = "SQL"
}
