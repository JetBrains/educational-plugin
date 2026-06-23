package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.EnvironmentAwareCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.learning.newproject.ui.EnvironmentAndNewCourseSettings
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsUI
import com.jetbrains.edu.python.learning.newproject.PyEnvironmentPresenter
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider

open class PyCourseBuilder : EnvironmentAwareCourseBuilder<PyLanguageEnvironment> {
  override fun taskTemplateName(course: Course): String? = PyConfigurator.TASK_PY
  override fun mainTemplateName(course: Course): String? = PyConfigurator.MAIN_PY
  override fun testTemplateName(course: Course): String? = PyConfigurator.TESTS_PY

  override fun getLanguageSettings(): LanguageSettings<PyLanguageEnvironment> = EnvironmentAndNewCourseSettings(
    getLanguageEnvironmentCatalogProvider(),
    PyEnvironmentPresenter,
    NewCourseSettingsUI.NoSettings
  )

  override fun getLanguageEnvironmentCatalogProvider(): LanguageEnvironmentCatalogProvider<PyLanguageEnvironment> =
    PyLanguageEnvironmentCatalogProvider()

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyLanguageEnvironment>? =
    object : CourseProjectGenerator<PyLanguageEnvironment>(this@PyCourseBuilder, course) {}

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return
      installRequiredPackages(project, projectSdk)
    }
  }
}
