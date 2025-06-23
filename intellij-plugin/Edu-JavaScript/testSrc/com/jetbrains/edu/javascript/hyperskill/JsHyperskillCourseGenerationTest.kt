package com.jetbrains.edu.javascript.hyperskill

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.javascript.learning.JsConfigurator.Companion.TASK_JS
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator.Companion.HYPERSKILL_TEST_DIR
import org.junit.Test

class JsHyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  @Test
  fun `test course structure creation`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = JavascriptLanguage,
      courseMode = CourseMode.EDUCATOR
    ) {}

    checkFileTree {
      dir("lesson1/task1") {
        dir(HYPERSKILL_TEST_DIR) {
          file("test.js")
        }
        file(TASK_JS)
        file("task.html")
      }
      file("package.json")
    }
  }
}