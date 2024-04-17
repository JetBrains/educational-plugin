package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.java.hyperskill.JHyperskillConfigurator.Companion.JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
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
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = JavaLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}