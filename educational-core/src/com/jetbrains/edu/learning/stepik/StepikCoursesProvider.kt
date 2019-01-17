package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader

class StepikCoursesProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    checkIsBackgroundThread()
    return if (isUnitTestMode) emptyList() else StepikCourseLoader.getCourseInfos()
  }
}
