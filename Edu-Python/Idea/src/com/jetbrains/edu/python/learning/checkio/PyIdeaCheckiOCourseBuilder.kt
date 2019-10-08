package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyIdeaLanguageSettings
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyIdeaCheckiOCourseBuilder : PyCheckiOCourseBuilderBase() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyIdeaLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyIdeaCheckiOCourseProjectGenerator(this, course)
}
