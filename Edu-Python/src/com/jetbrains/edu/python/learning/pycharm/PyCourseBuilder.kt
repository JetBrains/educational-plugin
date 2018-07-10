package com.jetbrains.edu.python.learning.pycharm

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCourseBuilder : PyCourseBuilder() {

  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> =
          PyLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
          PyCourseProjectGenerator(this, course)
}
