package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtCourseBuilder.Companion.getKotlinTemplateVariables
import com.jetbrains.edu.kotlin.hyperskill.KtHyperskillConfigurator.Companion.KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME

class KtHyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  fun `test course structure creation`() {
    courseWithFiles(courseProducer = ::HyperskillCourse, language = KotlinLanguage.INSTANCE, courseMode = CCUtils.COURSE_MODE,
                    settings = JdkProjectSettings.emptySettings()) {}

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

  fun `test build gradle file`() {
    courseWithFiles(courseProducer = ::HyperskillCourse,
                    language = KotlinLanguage.INSTANCE,
                    settings = JdkProjectSettings.emptySettings()) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME,
                                                                            getKotlinTemplateVariables())

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }
}