package com.jetbrains.edu.learning.compatibility

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.plugins.PluginInfo

class SqlCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)) return null
    @Suppress("UnstableApiUsage")
    if (!PlatformUtils.isCommercialEdition()) return null
    return listOf(PluginInfo.SQL)
  }

  override val technologyName: String = "SQL"
}
