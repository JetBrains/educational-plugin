package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.PlatformUtils.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.plugins.PluginInfo

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    val build = ApplicationInfo.getInstance().build
    return when {
      isPyCharmPro() -> if (build < BUILD_193) emptyList() else listOf(PluginInfo.PYTHON_PRO)
      isPyCharm() -> if (build < BUILD_193) emptyList() else listOf(PluginInfo.PYTHON_COMMUNITY)
      isIdeaUltimate() -> listOf(PluginInfo.PYTHON_PRO)
      isCLion() || isIntelliJ() || EduUtils.isAndroidStudio() -> listOf(PluginInfo.PYTHON_COMMUNITY)
      else -> null
    }
  }

  companion object {
    // BACKCOMPAT: 2019.3
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")!!
  }
}
