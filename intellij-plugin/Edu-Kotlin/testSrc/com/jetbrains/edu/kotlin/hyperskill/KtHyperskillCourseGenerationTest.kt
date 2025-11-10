package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.LEGACY_TEMPLATE_PREFIX
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase.Companion.getKotlinTemplateVariables
import com.jetbrains.edu.kotlin.hyperskill.KtHyperskillConfigurator.Companion.KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.junit.Test

class KtHyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  @Test
  fun `test course structure creation`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = KotlinLanguage.INSTANCE,
      courseMode = CourseMode.EDUCATOR
    ) {}

    checkFileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("Task.kt")
        }
        dir("test") {
          file("Tests.kt")
        }
        file("task.html")
      }
      file("build.gradle")
      file("settings.gradle")
    }
  }

  @Test
  fun `test build gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = KotlinLanguage.INSTANCE
    ) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(templateName, getKotlinTemplateVariables())

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = KotlinLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val templateName = LEGACY_TEMPLATE_PREFIX + HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(templateName)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}