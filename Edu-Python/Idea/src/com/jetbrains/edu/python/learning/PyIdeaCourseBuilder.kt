package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyIdeaCourseBuilder : PyCourseBuilderBase() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyIdeaLanguageSettings()
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyIdeaCourseProjectGenerator(this, course)
}
