package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCourseBuilderTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("tests.kt")
          }
        }
        dir("task2") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
            file("tests.kt")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("tests.kt")
          }
        }
      }
      dir("util") {
        dir("src") {
          file("koansTestUtil.kt")
          file("EduTestRunner.java")
        }
      }
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(KotlinLanguage.INSTANCE)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Tests.kt")
          }
        }
      }
      dir("util") {
        dir("src") {
          file("EduTestRunner.java")
        }
      }
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
