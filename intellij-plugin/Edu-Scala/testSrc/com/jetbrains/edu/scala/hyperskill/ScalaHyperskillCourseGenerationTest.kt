package com.jetbrains.edu.scala.hyperskill

import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.LEGACY_TEMPLATE_PREFIX
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.scala.hyperskill.ScalaHyperskillConfigurator.Companion.SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaHyperskillCourseGenerationTest : EduTestCase() {
  @Test
  fun `test build gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = ScalaLanguage.INSTANCE
    ) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(templateName)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = ScalaLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(templateName)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}