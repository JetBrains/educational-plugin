package com.jetbrains.edu.java

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newCourse

class JCourseGeneratorTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = JCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/java_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("Test.java")
          }
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
            file("Test.java")
          }
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("Test.java")
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

  fun `test new course structure`() {
    val course = newCourse(JavaLanguage.INSTANCE)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
            file("Test.java")
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
