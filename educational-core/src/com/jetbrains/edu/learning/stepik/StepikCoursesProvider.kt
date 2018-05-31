package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.EduCoursesProvider
import com.jetbrains.edu.learning.courseFormat.Course

class StepikCoursesProvider : EduCoursesProvider {
  override fun loadCourses(): List<Course> {
    check(!ApplicationManager.getApplication().isDispatchThread) { "Long running operation `loadCourses` invoked on UI thread" }
    return if (ApplicationManager.getApplication().isUnitTestMode) emptyList() else StepikConnector.getCourses(null)
  }
}
