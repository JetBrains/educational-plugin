package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCourseBuilderTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = KtCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json")
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world!") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
        }
        dir("Java to Kotlin conversion") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
        }
      }
      dir("Conventions") {
        dir("Comparison") {
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
      }
      file("build.gradle")
      file("settings.gradle")
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
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json", CourseType.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world!") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
        dir("Java to Kotlin conversion") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("Conventions") {
        dir("Comparison") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("util") {
        dir("src") {
          file("koansTestUtil.kt")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }
}
