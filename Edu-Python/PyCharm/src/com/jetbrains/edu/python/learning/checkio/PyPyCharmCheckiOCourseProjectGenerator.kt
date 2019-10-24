package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.reformatCodeInAllTaskFiles
import com.jetbrains.edu.python.learning.PyCourseBuilderBase
import com.jetbrains.edu.python.learning.PyPyCharmCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyPyCharmCheckiOCourseProjectGenerator(builder: PyCourseBuilderBase, val course: Course) :
  PyPyCharmCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean = true

  override fun afterProjectGenerated(project: Project, settings: PyNewProjectSettings) {
    super.afterProjectGenerated(project, settings)
    reformatCodeInAllTaskFiles(project, course)
  }
}
