package com.jetbrains.edu.learning.checkio

import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.newProjectUI.CheckiOCoursesPanel
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon
import kotlin.math.max

class CheckiOPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CheckiOPlatformProvider())
}

class CheckiOPlatformProvider : CoursesPlatformProvider() {
  override val name: String = CheckiONames.CHECKIO

  override val icon: Icon get() = EducationalCoreIcons.CheckiO.to24()

  override fun createPanel(scope: CoroutineScope): CoursesPanel = CheckiOCoursesPanel(this, scope)

  override suspend fun loadCourses(): List<CoursesGroup> {
    return if (EduUtils.isAndroidStudio()) {
      emptyList()
    }
    else {
      val courses = listOf(
        CheckiOCourse(CheckiONames.PY_CHECKIO, "${EduNames.PYTHON} ${EduNames.PYTHON_3_VERSION}"),
        CheckiOCourse(CheckiONames.JS_CHECKIO, EduNames.JAVASCRIPT)
      ).filter {
        val compatibility = it.compatibility
        compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
      }

      CoursesGroup(courses).asList()
    }
  }

  private fun Icon.to24() = IconUtil.scale(this, null, JBUIScale.scale(24f / max(iconHeight, iconWidth)))
}