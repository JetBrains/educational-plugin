package com.jetbrains.edu.learning.compatibility

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.PlatformUtils.*
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import javax.swing.Icon

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    @Suppress("DEPRECATION", "UnstableApiUsage")
    return when {
      isPyCharmPro() -> listOf(PluginInfos.PYTHON_PRO)
      // BACKCOMPAT: 2023.2. Merge with the following branch as `isPyCharm() || isDataSpell()`
      // Actually, `isPyCharm()` covers DataSpell case as well, so `isDataSpell()` is only for readability improvement
      isDataSpell() -> if (isDataSpellSupported) listOf(PluginInfos.PYTHON_COMMUNITY) else null
      isPyCharm() -> listOf(PluginInfos.PYTHON_COMMUNITY)
      isIdeaUltimate() -> listOf(PluginInfos.PYTHON_PRO)
      isCLion() || isIntelliJ() || EduUtilsKt.isAndroidStudio() -> listOf(PluginInfos.PYTHON_COMMUNITY)
      else -> null
    }
  }

  override val technologyName: String get() = "Python"
  override val logo: Icon get() = EducationalCoreIcons.PythonLogo
}

// JetBrains Academy courses didn't work with old versions of DataSpell,
// so let's enable support only since 2023.3
// BACKCOMPAT: 2023.2
val isDataSpellSupported: Boolean get() = ApplicationInfo.getInstance().build >= BUILD_233

// BACKCOMPAT: 2023.2
private val BUILD_233: BuildNumber = BuildNumber.fromString("233")!!
