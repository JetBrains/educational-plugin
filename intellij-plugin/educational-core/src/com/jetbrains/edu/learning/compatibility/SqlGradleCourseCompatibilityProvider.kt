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
    // We need SQL plugin available only in paid IDEs and Java + Gradle plugins available in IntelliJ IDEA and Android Studio.
    // So it's only IDEA Ultimate
    if (!PlatformUtils.isIdeaUltimate()) return null
    return listOf(
      PluginInfos.SQL,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String = "SQL"
}
