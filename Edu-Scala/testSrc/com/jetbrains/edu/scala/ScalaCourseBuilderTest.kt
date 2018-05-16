package com.jetbrains.edu.scala

import com.jetbrains.edu.learning.CourseGenerationTestBase
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaCourseBuilderTest : CourseGenerationTestBase<JdkProjectSettings>() {

  override val courseBuilder: EduCourseBuilder<JdkProjectSettings> = ScalaCourseBuilder()
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/scala_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("my task 1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
        }
        dir("my task 2") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(ScalaLanguage.INSTANCE)
    createCourseStructure(courseBuilder, course, defaultSettings)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
