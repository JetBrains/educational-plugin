package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyPyCharmCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyPyCharmHyperskillCourseBuilder : PyPyCharmCourseBuilder() {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings> =
    PyPyCharmHyperskillCourseProjectGenerator(this, course)
}
