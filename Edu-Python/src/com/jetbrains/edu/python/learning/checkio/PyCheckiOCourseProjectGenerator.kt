package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.reformatCodeInAllTaskFiles
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyCheckiOCourseProjectGenerator(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean = true

  override fun afterProjectGenerated(project: Project, projectSettings: PyNewProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    reformatCodeInAllTaskFiles(project, course)
  }
}
