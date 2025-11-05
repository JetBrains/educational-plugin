package com.jetbrains.edu.kotlin.courseGeneration

import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test
import kotlin.test.assertContains

class KtCourseBuilderTest : JvmCourseGenerationTestBase() {

  @Test
  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json")
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world") {
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
      dir("gradle") {
        dir("wrapper") {
          file("gradle-wrapper.properties")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }

  @Test
  fun `test new course structure`() {
    val course = newCourse(KotlinLanguage.INSTANCE)
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  @Test
  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json", CourseMode.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world") {
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

  @Test
  fun `Kotlin version in auto-generated build_gradle has form Major_dot_Minor_dot_Patch`() {
    val course = course(language = KotlinLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR) {}
    createCourseStructure(course)

    val text = findFile("build.gradle").readText()

    assertContains(text, """kotlin_version\s*=\s*['"](\d+\.\d+\.\d+)['"]""".toRegex(), "Kotlin version must be of the form major.minor.patch")
  }
}
