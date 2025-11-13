package com.jetbrains.edu.jvm.courseGeneration

import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.LEGACY_TEMPLATE_PREFIX
import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.junit.Test
import kotlin.test.assertContains

abstract class GradleScriptsGenerationTestBase : JvmCourseGenerationTestBase() {
  protected abstract val defaultBuildGradleTemplateName: String

  protected abstract fun createCourse(courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit): Course

  @Test
  fun `test build scripts are taken from archive if they are present`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("build.gradle", BUILD_GRADLE_TEXT)
      additionalFile("settings.gradle", SETTINGS_GRADLE_TEXT)
    }
    createCourseStructure(course)

    val templateVariables = course.gradleCourseBuilder.templateVariables(project.courseDir.name)

    val expectedBuildGradleText = BUILD_GRADLE_TEXT
    val expectedSettingsGradleText = SETTINGS_GRADLE_TEXT

    val expectedGradleWrapperPropertiesText = GeneratorUtils.getInternalTemplateText(GRADLE_WRAPPER_PROPERTIES, templateVariables)

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to expectedGradleWrapperPropertiesText
    )
  }

  @Test
  fun `test legacy script files are generated if there is no build scripts files in archive`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {}
    createCourseStructure(course)

    val templateVariables = course.gradleCourseBuilder.templateVariables(project.courseDir.name)

    val buildTemplateName = LEGACY_TEMPLATE_PREFIX + defaultBuildGradleTemplateName
    val expectedBuildGradleText = GeneratorUtils.getInternalTemplateText(buildTemplateName, templateVariables)

    val settingsTemplateName = LEGACY_TEMPLATE_PREFIX + SETTINGS_FILE_NAME
    val expectedSettingsGradleText = GeneratorUtils.getInternalTemplateText(settingsTemplateName, templateVariables)

    val expectedGradleWrapperPropertiesText = GeneratorUtils.getInternalTemplateText(GRADLE_WRAPPER_PROPERTIES, templateVariables)

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to expectedGradleWrapperPropertiesText
    )
  }

  @Test
  fun `test legacy build script file is generated if it is missing in archive`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("settings.gradle", SETTINGS_GRADLE_TEXT)
    }
    createCourseStructure(course)

    val templateVariables = course.gradleCourseBuilder.templateVariables(project.courseDir.name)

    val buildTemplateName = LEGACY_TEMPLATE_PREFIX + defaultBuildGradleTemplateName
    val expectedBuildGradleText = GeneratorUtils.getInternalTemplateText(buildTemplateName, templateVariables)
    val expectedSettingsGradleText = SETTINGS_GRADLE_TEXT

    val expectedGradleWrapperPropertiesText = GeneratorUtils.getInternalTemplateText(GRADLE_WRAPPER_PROPERTIES, templateVariables)

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to expectedGradleWrapperPropertiesText
    )
  }

  @Test
  fun `test legacy settings script file is generated if it is missing in archive`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("build.gradle", BUILD_GRADLE_TEXT)
    }
    createCourseStructure(course)

    val templateVariables = course.gradleCourseBuilder.templateVariables(project.courseDir.name)

    val expectedBuildGradleText = BUILD_GRADLE_TEXT
    val settingsTemplateName = LEGACY_TEMPLATE_PREFIX + SETTINGS_FILE_NAME
    val expectedSettingsGradleText = GeneratorUtils.getInternalTemplateText(settingsTemplateName, templateVariables)

    val expectedGradleWrapperPropertiesText = GeneratorUtils.getInternalTemplateText(GRADLE_WRAPPER_PROPERTIES, templateVariables)

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to expectedGradleWrapperPropertiesText
    )
  }

  @Test
  fun `test new build script file is generated for CC`() {
    val course = createCourse(courseMode = CourseMode.EDUCATOR) {}
    createCourseStructure(course)

    val templateVariables = course.gradleCourseBuilder.templateVariables(project.courseDir.name)

    val buildTemplateName = defaultBuildGradleTemplateName
    val expectedBuildGradleText = GeneratorUtils.getInternalTemplateText(buildTemplateName, templateVariables)

    val settingsTemplateName = SETTINGS_FILE_NAME
    val expectedSettingsGradleText = GeneratorUtils.getInternalTemplateText(settingsTemplateName, templateVariables)

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to null
    )
  }

  @Test
  fun `test build gradle scripts are not generated if build gradle kts scripts are present`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("build.gradle.kts", BUILD_GRADLE_TEXT)
      additionalFile("settings.gradle.kts", SETTINGS_GRADLE_TEXT)
      additionalFile("gradle/wrapper/gradle-wrapper.properties", GRADLE_WRAPPER_PROPERTIES_TEXT)
    }
    createCourseStructure(course)

    assertListOfAdditionalFiles(
      course,
      "build.gradle.kts" to BUILD_GRADLE_TEXT,
      "settings.gradle.kts" to SETTINGS_GRADLE_TEXT,
      "gradle/wrapper/gradle-wrapper.properties" to GRADLE_WRAPPER_PROPERTIES_TEXT
    )
  }

  @Test
  fun `test gradle wrapper properties file is taken from archive if present`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("build.gradle", BUILD_GRADLE_TEXT)
      additionalFile("settings.gradle", SETTINGS_GRADLE_TEXT)
      additionalFile("gradle/wrapper/gradle-wrapper.properties", GRADLE_WRAPPER_PROPERTIES_TEXT)
    }
    createCourseStructure(course)

    val expectedBuildGradleText = BUILD_GRADLE_TEXT
    val expectedSettingsGradleText = SETTINGS_GRADLE_TEXT
    val expectedGradleWrapperPropertiesText = GRADLE_WRAPPER_PROPERTIES_TEXT

    assertListOfAdditionalFiles(
      course,
      "build.gradle" to expectedBuildGradleText,
      "settings.gradle" to expectedSettingsGradleText,
      "gradle/wrapper/gradle-wrapper.properties" to expectedGradleWrapperPropertiesText
    )
  }

  @Test
  fun `test gradle wrapper properties file is generated with gradle version 8`() {
    val course = createCourse(courseMode = CourseMode.STUDENT) {
      additionalFile("build.gradle", BUILD_GRADLE_TEXT)
      additionalFile("settings.gradle", SETTINGS_GRADLE_TEXT)
    }
    createCourseStructure(course)

    val text = findFile("gradle/wrapper/gradle-wrapper.properties").readText()

    assertContains(text, "distributionUrl=.*gradle-8.14.3-bin.zip".toRegex(), "Generated gradle wrapper properties file should contain `distributionUrl` property with version 8.14.3")
  }

  private val Course.gradleCourseBuilder: GradleCourseBuilderBase
    get() = configurator?.courseBuilder as GradleCourseBuilderBase

  companion object {
    private const val BUILD_GRADLE_TEXT = "build.gradle text"
    private const val SETTINGS_GRADLE_TEXT = "settings.gradle text"
    private const val GRADLE_WRAPPER_PROPERTIES_TEXT = "gradle-wrapper.properties text"
  }
}