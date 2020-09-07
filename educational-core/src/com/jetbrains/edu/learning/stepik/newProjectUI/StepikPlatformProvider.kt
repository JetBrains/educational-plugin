package com.jetbrains.edu.learning.stepik.newProjectUI

import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class StepikPlatformProvider(private val coursesProvider: StepikCoursesProvider) : CoursesPlatformProvider() {

  override val name: String = StepikNames.STEPIK

  override val icon: Icon get() = EducationalCoreIcons.StepikCourseTab

  override fun createPanel(scope: CoroutineScope): CoursesPanel = StepikCoursesPanel(this, scope)

  override suspend fun loadCourses(): List<StepikCourse> {
    checkIsBackgroundThread()
    return if (isUnitTestMode) {
      return emptyList()
    }
    else {
      coursesProvider.getStepikCourses()
    }
  }
}