package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyIdeaCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyIdeaHyperskillCourseBuilder : PyIdeaCourseBuilder() {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings> =
    PyIdeaHyperskillCourseProjectGenerator(this, course)
}
