package com.jetbrains.edu.java

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

class JCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/java_course.json")
    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(JavaLanguage.INSTANCE)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("task.html")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/java_course.json", CourseType.EDUCATOR)
    val expectedFileTree = fileTree {
      dir(".idea") {}
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("task.html")
          }
          dir("test") {
            file("Test.java")
          }
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
            file("task.html")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("task.html")
          }
          dir("test") {
            file("Test.java")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }
}
