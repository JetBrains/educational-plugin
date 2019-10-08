package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyPyCharmNewCourseBuilder : PyNewCourseBuilderBase() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyPyCharmLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? {
    return object : PyPyCharmCourseProjectGenerator(this, course) {
      override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {}
    }
  }
}
