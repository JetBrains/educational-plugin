package com.jetbrains.edu.python.learning

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.MAIN_PY
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TASK_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FILE_NAME
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FOLDER
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyFakeSdkType
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import com.jetbrains.python.PyNames
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

class PyNewCourseBuilder : EduCourseBuilder<PyProjectSettings> {
  override fun taskTemplateName(course: Course): String = TASK_PY
  override fun mainTemplateName(course: Course): String = MAIN_PY
  override fun testTemplateName(course: Course): String = TEST_FILE_NAME

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getDefaultSettings(): Result<PyProjectSettings, String> {
    return findPath(INTERPRETER_PROPERTY, "Python interpreter").flatMap { sdkPath ->
      val versionString = PythonSdkFlavor.getApplicableFlavors(false)[0].getVersionString(sdkPath)
      val sdk = ProjectJdkImpl(versionString, PyFakeSdkType, sdkPath, versionString)
      Ok(PyProjectSettings(sdk))
    }
  }

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings> {
    return object : PyCourseProjectGenerator(this@PyNewCourseBuilder, course) {
      override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {}
    }
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

  companion object {
    private const val INTERPRETER_PROPERTY = "project.python.interpreter"
  }
}
