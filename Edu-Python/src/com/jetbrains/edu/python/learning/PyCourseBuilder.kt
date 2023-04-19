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
  override val taskTemplateName: String? = PyConfigurator.TASK_PY
  override val mainTemplateName: String? = PyConfigurator.MAIN_PY
  override val testTemplateName: String? = PyConfigurator.TESTS_PY

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getSupportedLanguageVersions(): List<String> = getSupprotedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings>? =
    PyCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return
      installRequiredPackages(project, projectSdk)
    }
  }
}
