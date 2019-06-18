package com.jetbrains.edu.python.learning.pycharm

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyNewCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyNewCourseBuilder : PyNewCourseBuilder() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? {
    return object : PyCourseProjectGenerator(this, course) {
      override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {}
    }
  }
}
