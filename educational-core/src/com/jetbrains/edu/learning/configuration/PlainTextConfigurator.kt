@file:Suppress("HardCodedStringLiteral")

package com.jetbrains.edu.learning.configuration

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import javax.swing.Icon


open class PlainTextConfigurator : EduConfigurator<EmptyProjectSettings> {
  override val courseBuilder: EduCourseBuilder<EmptyProjectSettings>
    get() = PlainTextCourseBuilder()

  override val testFileName: String
    get() = "Tests.txt"

  override fun getMockFileName(course: Course, text: String): String = "Task.txt"

  override val testDirs: List<String>
    get() = listOf(TEST_DIR_NAME)

  override val isEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal || isUnitTestMode

  override val logo: Icon
    get() = AllIcons.FileTypes.Text

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PlainTextTaskCheckerProvider()

  override val isCourseCreatorEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal || isUnitTestMode

  companion object {
    const val TEST_DIR_NAME = "tests"
  }
}

class PlainTextCourseBuilder : EduCourseBuilder<EmptyProjectSettings> {
  override fun taskTemplateName(course: Course): String = "Task.txt"
  override fun mainTemplateName(course: Course): String = "Main.txt"
  override fun testTemplateName(course: Course): String = "Tests.txt"

  override fun getLanguageSettings(): LanguageSettings<EmptyProjectSettings> = object : LanguageSettings<EmptyProjectSettings>() {
    override fun getSettings(): EmptyProjectSettings = EmptyProjectSettings
  }

  override fun getDefaultSettings(): Result<EmptyProjectSettings, String> = Ok(EmptyProjectSettings)

  override fun getSupportedLanguageVersions(): List<String> = listOf("1.42")

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<EmptyProjectSettings> = PlainTextCourseGenerator(this, course)
}

class PlainTextCourseGenerator(
  builder: EduCourseBuilder<EmptyProjectSettings>,
  course: Course
) : CourseProjectGenerator<EmptyProjectSettings>(builder, course)
