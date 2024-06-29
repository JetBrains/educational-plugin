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

// BACKCOMPAT: 2023.3
private val BUILD_241 = BuildNumber.fromString("241")!!

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    val requiredPlugins = mutableListOf<PluginInfo>()
    @Suppress("DEPRECATION", "UnstableApiUsage")
    requiredPlugins += when {
      isPyCharmPro() -> PYTHON_PRO
      isPyCharm() || isDataSpell() -> PYTHON_COMMUNITY
      isIdeaUltimate() -> PYTHON_PRO
      isCLion() || isIntelliJ() || EduUtilsKt.isAndroidStudio() -> PYTHON_COMMUNITY
      else -> return null
    }
    if (ApplicationInfo.getInstance().build >= BUILD_241) {
      requiredPlugins += TOML
    }
    return requiredPlugins
  }

  override val technologyName: String get() = "Python"
  override val logo: Icon get() = EducationalCoreIcons.Language.PythonLogo
}
