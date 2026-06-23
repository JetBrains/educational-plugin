package com.jetbrains.edu.python.learning

import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider
import com.jetbrains.edu.learning.newproject.ui.EnvironmentAndNewCourseSettings
import com.jetbrains.edu.learning.newproject.ui.newCourseSettings.NewCourseSettingsUI
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.MAIN_PY
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TASK_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FILE_NAME
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FOLDER
import com.jetbrains.edu.python.learning.newproject.PyEnvironmentPresenter
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider
import com.jetbrains.python.PyNames

class PyNewCourseBuilder : EnvironmentAwareCourseBuilder<PyLanguageEnvironment> {
  override fun taskTemplateName(course: Course): String = TASK_PY
  override fun mainTemplateName(course: Course): String = MAIN_PY
  override fun testTemplateName(course: Course): String = TEST_FILE_NAME

  override fun getLanguageSettings(): LanguageSettings<PyLanguageEnvironment> = EnvironmentAndNewCourseSettings(
    getLanguageEnvironmentCatalogProvider(),
    PyEnvironmentPresenter,
    NewCourseSettingsUI.NoSettings
  )

  override fun getLanguageEnvironmentCatalogProvider(): LanguageEnvironmentCatalogProvider<PyLanguageEnvironment> =
    PyLanguageEnvironmentCatalogProvider()

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyLanguageEnvironment> {
    return object : CourseProjectGenerator<PyLanguageEnvironment>(this@PyNewCourseBuilder, course) {}
  }

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = ArrayList(super.getDefaultTaskTemplates(course, info, withSources, withTests))
    if (withSources) {
      templates += TemplateFileInfo(PyNames.INIT_DOT_PY, PyNames.INIT_DOT_PY, false)
    }
    if (withTests) {
      templates += TemplateFileInfo(PyNames.INIT_DOT_PY, "$TEST_FOLDER/${PyNames.INIT_DOT_PY}", false)
    }
    return templates
  }
}
