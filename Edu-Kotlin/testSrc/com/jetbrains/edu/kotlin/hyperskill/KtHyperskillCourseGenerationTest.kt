package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.FileTreeBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtHyperskillCourseGenerationTest : EduTestCase() {
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

  private fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }
}