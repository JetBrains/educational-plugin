package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyPyCharmLanguageSettings
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyPyCharmCheckiOCourseBuilder : PyCheckiOCourseBuilderBase() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyPyCharmLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyPyCharmCheckiOCourseProjectGenerator(this, course)
}
