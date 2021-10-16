package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtCourseBuilder.Companion.getKotlinTemplateVariables
import com.jetbrains.edu.kotlin.hyperskill.KtHyperskillConfigurator.Companion.KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME

class KtHyperskillCourseGenerationTest : EduTestCase() {
  fun `test course structure creation`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      courseWithFiles(courseProducer = ::HyperskillCourse, language = KotlinLanguage.INSTANCE, courseMode = CCUtils.COURSE_MODE,
                      settings = JdkProjectSettings.emptySettings()) {}
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
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

  private fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }
}