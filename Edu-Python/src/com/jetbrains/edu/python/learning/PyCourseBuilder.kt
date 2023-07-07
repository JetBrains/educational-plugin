package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

open class PyCourseBuilder : EduCourseBuilder<PyProjectSettings> {
  override fun taskTemplateName(course: Course): String? = PyConfigurator.TASK_PY
  override fun mainTemplateName(course: Course): String? = PyConfigurator.MAIN_PY
  override fun testTemplateName(course: Course): String? = PyConfigurator.TESTS_PY

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings>? =
    PyCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return
      installRequiredPackages(project, projectSdk)
    }
  }
}
