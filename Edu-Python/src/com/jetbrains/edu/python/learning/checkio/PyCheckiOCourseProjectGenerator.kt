package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.reformatCodeInAllTaskFiles
import com.jetbrains.edu.python.learning.PyCourseBuilder
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

class PyCheckiOCourseProjectGenerator(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: PyProjectSettings, onConfigurationFinished: () -> Unit) {
    reformatCodeInAllTaskFiles(project, course)
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }
}
