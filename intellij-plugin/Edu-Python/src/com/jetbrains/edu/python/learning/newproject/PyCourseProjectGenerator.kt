package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class PyCourseProjectGenerator(
  builder: EduCourseBuilder<PyProjectSettings>,
  course: Course
) : CourseProjectGenerator<PyProjectSettings>(builder, course) {

  override suspend fun afterProjectGenerated(
    project: Project,
    projectSettings: PyProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    afterPyProjectGeneration(project, projectSettings) {
      super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
    }
  }

  companion object {
    val LOG = logger<PyCourseProjectGenerator>()
  }
}
