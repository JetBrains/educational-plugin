package com.jetbrains.edu.learning.checkio

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.newProjectUI.CheckiOCoursesPanel
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.*
import icons.EducationalCoreIcons
import javax.swing.Icon

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
}