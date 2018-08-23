package com.jetbrains.edu.python.learning.checkio.pycharm

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.checkio.PyCheckiOCourseBuilder
import com.jetbrains.edu.python.learning.pycharm.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOCourseBuilder : PyCheckiOCourseBuilder() {
  private val myPyCourseBuilder = PyCourseBuilder()

  override fun getLanguageSettings() = myPyCourseBuilder.languageSettings

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyCheckiOCourseProjectGenerator(this, course)
}
