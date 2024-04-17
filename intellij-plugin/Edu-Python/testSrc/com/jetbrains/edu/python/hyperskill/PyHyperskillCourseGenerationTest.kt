package com.jetbrains.edu.python.hyperskill

import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator.Companion.HYPERSKILL_TEST_DIR
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TASK_PY
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TESTS_PY
import com.jetbrains.python.PythonLanguage
import org.junit.Test


class PyHyperskillCourseGenerationTest : EduTestCase() {

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtilsKt.isAndroidStudio()) {
      withDefaultHtmlTaskDescription {
        super.runTestRunnable(context)
      }
    }
  }

  @Test
  fun `test course structure creation`() {
    courseWithFiles(courseProducer = ::HyperskillCourse, language = PythonLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR) {}

    checkFileTree {
      dir("lesson1/task1") {
        file(TASK_PY)
        dir(HYPERSKILL_TEST_DIR) {
          file(TESTS_PY)
        }
        file("task.html")
      }
    }
  }
}
