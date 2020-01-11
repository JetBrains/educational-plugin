package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.reformatCodeInAllTaskFiles
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.PyIdeaCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyIdeaCheckiOCourseProjectGenerator(builder: PyCourseBuilder, val course: Course) :
  PyIdeaCourseProjectGenerator(builder, course) {

  override fun beforeProjectGenerated(): Boolean = true

  override fun afterProjectGenerated(project: Project, settings: PyNewProjectSettings) {
    super.afterProjectGenerated(project, settings)
    reformatCodeInAllTaskFiles(project, course)
  }
}
