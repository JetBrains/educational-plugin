package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import com.jetbrains.edu.learning.isFeatureEnabled

class SqlGradleCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)) return null
    @Suppress("UnstableApiUsage")
    if (!PlatformUtils.isCommercialEdition()) return null
    return listOf(
      PluginInfos.SQL,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String = "SQL"
}
