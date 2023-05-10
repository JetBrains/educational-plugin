package com.jetbrains.edu.python.learning

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
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
  override val taskTemplateName: String = TASK_PY
  override val mainTemplateName: String = MAIN_PY
  override val testTemplateName: String = TEST_FILE_NAME

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getDefaultSettings(): Result<PyProjectSettings, String> {
    val sdkPath = System.getProperty(INTERPRETER_PROPERTY)
      ?: return Err("Failed to find Python interpreter because `$INTERPRETER_PROPERTY` system property is not provided")

    val versionString = PythonSdkFlavor.getApplicableFlavors(false)[0].getVersionString(sdkPath)

    val sdk = ProjectJdkImpl(versionString, PyFakeSdkType, sdkPath, versionString)
    return Ok(PyProjectSettings(sdk))
  }

  override fun getSupportedLanguageVersions(): List<String> = getSupprotedVersions()

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
