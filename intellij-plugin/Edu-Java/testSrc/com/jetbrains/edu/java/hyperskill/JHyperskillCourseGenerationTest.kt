package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.java.hyperskill.JHyperskillConfigurator.Companion.JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.LEGACY_TEMPLATE_PREFIX
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.junit.Test

class JHyperskillCourseGenerationTest : EduTestCase() {
  @Test
  fun `test build gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = JavaLanguage.INSTANCE
    ) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(templateName)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = JavaLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(templateName)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}