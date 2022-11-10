package com.jetbrains.edu.scala.courseGeneration

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaSbtCourseBuilderTest : JvmCourseGenerationTestBase() {

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    // https://youtrack.jetbrains.com/issue/SCL-20726
    if (ApplicationInfo.getInstance().build < BUILD_223) {
      super.runTestRunnable(testRunnable)
    }
  }

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/scala_course_sbt.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("TestSpec.scala")
          }
          file("task.html")
          file("build.sbt")
        }
      }
      dir("project") {
        file("build.properties")
      }
      file("build.sbt")
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(ScalaLanguage.INSTANCE, environment = "sbt")
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("TestSpec.scala")
          }
          file("task.md")
          file("build.sbt")
        }
      }
      dir("project") {
        file("build.properties")
      }
      file("build.sbt")
    }

    expectedFileTree.assertEquals(rootDir)
  }

  companion object {
    private val BUILD_223: BuildNumber = BuildNumber.fromString("223")!!
  }
}
