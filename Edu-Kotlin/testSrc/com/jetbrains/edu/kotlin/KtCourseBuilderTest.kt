package com.jetbrains.edu.kotlin

import com.jetbrains.edu.kotlin.studio.KtCourseBuilder
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
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
          }
          dir("test") {
            file("tests.kt")
          }
        }
        dir("task2") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
        }
      }
      dir("util") {
        dir("src") {
          file("koansTestUtil.kt")
        }
        dir("test") {
        }
      }
      gradleFiles()
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
          }
          dir("test") {
            file("Tests.kt")
          }
        }
      }
      gradleFiles()
    }

    expectedFileTree.assertEquals(rootDir)
  }

  private fun FileTreeBuilder.gradleFiles() {
    dir("gradle") {
      dir("wrapper") {
        file("gradle-wrapper.jar")
        file("gradle-wrapper.properties")
      }
    }
    file("gradlew")
    file("gradlew.bat")
    file("build.gradle")
    file("settings.gradle")
  }
}
