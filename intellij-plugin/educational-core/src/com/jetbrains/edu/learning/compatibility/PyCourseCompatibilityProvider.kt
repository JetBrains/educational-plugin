package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.PlatformUtils.*
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos.PYTHON_COMMUNITY
import com.jetbrains.edu.learning.courseFormat.PluginInfos.PYTHON_PRO
import com.jetbrains.edu.learning.courseFormat.PluginInfos.TOML
import javax.swing.Icon

// BACKCOMPAT: 2024.1
private val BUILD_242 = BuildNumber.fromString("242")!!

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    val requiredPlugins = mutableListOf<PluginInfo>()
    @Suppress("DEPRECATION", "UnstableApiUsage")
    requiredPlugins += when {
      // BACKCOMPAT: 2024.1. `isPyCharmPro() || isIdeaUltimate()` branch can be dropped
      // since they are covered by `isPyCharm()` and `isIntelliJ()` below
      isPyCharmPro() || isIdeaUltimate() -> if (ApplicationInfo.getInstance().build >= BUILD_242) PYTHON_COMMUNITY else PYTHON_PRO
      // Actually, `isPyCharm()` covers DataSpell case as well, so `isDataSpell()` is only for readability improvement
      isPyCharm() || isDataSpell() ||
      isCLion() || isIntelliJ() || EduUtilsKt.isAndroidStudio() -> PYTHON_COMMUNITY
      else -> return null
    }
    requiredPlugins += TOML
    return requiredPlugins
  }

  override val technologyName: String get() = "Python"
  override val logo: Icon get() = EducationalCoreIcons.Language.Python
}
