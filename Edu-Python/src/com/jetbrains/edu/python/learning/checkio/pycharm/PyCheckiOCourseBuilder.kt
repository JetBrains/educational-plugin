package com.jetbrains.edu.python.learning.checkio.pycharm

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.checkio.PyCheckiOCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOCourseBuilder : PyCheckiOCourseBuilder() {
  override fun getLanguageSettings() = PyCheckiOLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyCheckiOCourseProjectGenerator(this, course)
}
