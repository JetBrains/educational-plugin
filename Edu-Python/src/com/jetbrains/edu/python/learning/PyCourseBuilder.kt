package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyCourseBuilder : EduCourseBuilder<PyNewProjectSettings> {
  override val taskTemplateName: String? = PyConfigurator.TASK_PY
  override val testTemplateName: String? = PyConfigurator.TESTS_PY

  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyLanguageSettings()
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyCourseProjectGenerator(this, course)
}
