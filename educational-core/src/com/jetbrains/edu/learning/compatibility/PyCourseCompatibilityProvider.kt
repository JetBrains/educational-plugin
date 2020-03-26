package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.PlatformUtils.*
import com.jetbrains.edu.learning.EduUtils

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<String>? {
    val build = ApplicationInfo.getInstance().build
    return when {
      isPyCharmPro() -> if (build < BUILD_193) emptyList() else listOf(PYTHON_PRO_PLUGIN)
      isPyCharm() -> if (build < BUILD_193) emptyList() else listOf(PYTHON_COMMUNITY_PLUGIN)
      isIdeaUltimate() -> listOf(PYTHON_PRO_PLUGIN)
      isCLion() || isIntelliJ() || EduUtils.isAndroidStudio() -> listOf(PYTHON_COMMUNITY_PLUGIN)
      else -> null
    }
  }

  companion object {
    // BACKCOMPAT: 2019.3
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")!!

    private const val PYTHON_PRO_PLUGIN = "Pythonid"
    private const val PYTHON_COMMUNITY_PLUGIN = "PythonCore"
  }
}
