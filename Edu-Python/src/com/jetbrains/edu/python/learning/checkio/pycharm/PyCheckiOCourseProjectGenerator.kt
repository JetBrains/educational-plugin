package com.jetbrains.edu.python.learning.checkio.pycharm

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.checkio.PyCheckiOCourseProjectGenerator
import com.jetbrains.edu.python.learning.pycharm.PyCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

internal class PyCheckiOCourseProjectGenerator(builder: PyCourseBuilder, course: Course) : PyCheckiOCourseProjectGenerator(builder, course) {
  private val myPyCourseProjectGenerator = PyCourseProjectGenerator(builder, course)

  override fun afterProjectGenerated(project: Project, settings: PyNewProjectSettings) =
    myPyCourseProjectGenerator.afterProjectGenerated(project, settings)
}
