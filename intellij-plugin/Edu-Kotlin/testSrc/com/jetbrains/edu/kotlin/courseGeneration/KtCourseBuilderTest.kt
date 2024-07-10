package com.jetbrains.edu.kotlin.courseGeneration

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

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
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }

  @Test
  fun `test new course structure`() {
    // https://youtrack.jetbrains.com/issue/EDU-6934
    if (ApplicationInfo.getInstance().build >= BUILD_241) {
      return
    }

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

  companion object {
    private val BUILD_241 = BuildNumber.fromString("241")!!
  }
}
