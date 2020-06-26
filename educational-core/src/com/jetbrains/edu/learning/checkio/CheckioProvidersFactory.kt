package com.jetbrains.edu.learning.checkio

import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.newProjectUI.CheckiOCoursesPanel
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import icons.EducationalCoreIcons
import javax.swing.Icon
import kotlin.math.max

class CheckiOPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CheckiOPlatformProvider())
}

private class CheckiOPlatformProvider : CoursesPlatformProvider {
  override val name: String = CheckiONames.CHECKIO

  override val icon: Icon get() = EducationalCoreIcons.CheckiO.to24()

  override fun getPanel(dialog: BrowseCoursesDialog): CoursesPanel = CheckiOCoursesPanel(dialog, this)

  override suspend fun loadCourses(): List<Course> {
    return if (EduUtils.isAndroidStudio()) {
      emptyList()
    }
    else {
      listOf(
        CheckiOCourse(CheckiONames.PY_CHECKIO, "${EduNames.PYTHON} ${EduNames.PYTHON_3_VERSION}"),
        CheckiOCourse(CheckiONames.JS_CHECKIO, EduNames.JAVASCRIPT)
      )
    }
  }

  private fun Icon.to24() = IconUtil.scale(this, null, JBUIScale.scale(24f / max(iconHeight, iconWidth)))
}