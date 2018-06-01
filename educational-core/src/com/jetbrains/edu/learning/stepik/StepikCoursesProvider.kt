package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduCoursesProvider
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode

class StepikCoursesProvider : EduCoursesProvider {
  override fun loadCourses(): List<Course> {
    checkIsBackgroundThread()
    return if (isUnitTestMode) emptyList() else StepikConnector.getCourses(null)
  }
}
