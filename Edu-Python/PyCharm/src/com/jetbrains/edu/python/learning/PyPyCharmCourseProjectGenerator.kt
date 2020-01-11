package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyPyCharmCourseProjectGenerator(
  builder: EduCourseBuilder<PyNewProjectSettings>,
  course: Course
) : PyCourseProjectGenerator(builder, course)
