package com.jetbrains.edu.python.learning

import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.MAIN_PY
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TASK_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FILE_NAME
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FOLDER
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.PyNames
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyNewCourseBuilder : EduCourseBuilder<PyNewProjectSettings> {
  override val taskTemplateName: String = TASK_PY
  override val mainTemplateName: String = MAIN_PY
  override val testTemplateName: String = TEST_FILE_NAME

  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyLanguageSettings()

  override fun getSupportedLanguageVersions(): List<String> = getSupprotedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings> {
    return PyCourseProjectGenerator(this@PyNewCourseBuilder, course)
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
