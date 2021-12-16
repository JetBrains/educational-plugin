package com.jetbrains.edu.javascript.hyperskill

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.javascript.learning.JsConfigurator.Companion.TASK_JS
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator.Companion.HYPERSKILL_TEST_DIR
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JsHyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  fun `test course structure creation`() {
    courseWithFiles(courseProducer = ::HyperskillCourse, language = JavascriptLanguage.INSTANCE, courseMode = CCUtils.COURSE_MODE,
                    settings = JsNewProjectSettings()) {}

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