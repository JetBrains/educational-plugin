package com.jetbrains.edu.learning

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test

class HyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  @Test
  fun `test course structure creation`() {
    courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CourseMode.EDUCATOR) {}

    checkFileTree {
      dir("lesson1/task1") {
        dir("tests") {
          file("Tests.txt")
        }
        file("Task.txt")
        file("task.html")
      }
    }
  }
}