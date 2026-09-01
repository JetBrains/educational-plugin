package com.jetbrains.edu.yaml.inspections

import com.intellij.openapi.project.Project
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.configuration.DefaultEnvironmentSettings
import com.jetbrains.edu.learning.configuration.EnvironmentSettingValue
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configuration.defaultEnvironmentSettings
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.registerConfigurator
import org.intellij.lang.annotations.Language
import org.junit.Test

class EnvironmentSettingValueInspectionTest : YamlInspectionsTestBase(EnvironmentSettingValueInspection::class) {

  override fun setUp() {
    super.setUp()
    registerConfigurator<EnvironmentSettingsTestConfigurator>(PlainTextLanguage.INSTANCE, environment = TEST_ENVIRONMENT)
  }

  @Test
  fun `test conflicting course-defined value`() = testEnvironmentSettings("""
      title: Test Course
      summary: sum
      programming_language: Plain text
      environment: $TEST_ENVIRONMENT
      environment_settings:
        course_defined: <warning descr="The actual value should be 'expected_course_defined'. This value will be overwritten when the course archive is created.">unexpected</warning>
      content:
      - lesson1
    """)

  @Test
  fun `test matching course-defined value`() = testEnvironmentSettings("""
      title: Test Course
      summary: sum
      programming_language: Plain text
      environment: $TEST_ENVIRONMENT
      environment_settings:
        course_defined: expected_course_defined
      content:
      - lesson1
    """)

  @Test
  fun `test conflicting user-defined value`() = testEnvironmentSettings("""
      title: Test Course
      summary: sum
      programming_language: Plain text
      environment: $TEST_ENVIRONMENT
      environment_settings:
        user_defined: value_changed_by_user
      content:
      - lesson1
    """)

  @Test
  fun `test setting without default value`() = testEnvironmentSettings("""
      title: Test Course
      summary: sum
      programming_language: Plain text
      environment: $TEST_ENVIRONMENT
      environment_settings:
        another_setting: value
      content:
      - lesson1
    """)

  private fun testEnvironmentSettings(@Language("YAML") courseInfoYAMLConfig: String) {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, environment = TEST_ENVIRONMENT) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }

    testHighlighting(course, courseInfoYAMLConfig.trimIndent())
  }

  companion object {
    private const val TEST_ENVIRONMENT = "test-env"
  }
}

private class EnvironmentSettingsTestConfigurator : PlainTextConfigurator() {
  override fun getEnvironmentSettings(project: Project): DefaultEnvironmentSettings = defaultEnvironmentSettings(
    "course_defined" to EnvironmentSettingValue.course("expected_course_defined"),
    "user_defined" to EnvironmentSettingValue.user("expected_user_defined")
  )
}
