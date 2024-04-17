package com.jetbrains.edu.python.actions

import com.jetbrains.edu.coursecreator.actions.placeholder.CCAddAnswerPlaceholderActionTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyNewAddAnswerPlaceholderActionTest : CCAddAnswerPlaceholderActionTestBase() {

  @Test
  fun `test placeholder text`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = PythonLanguage.getInstance(),
                                 environment = "unittest") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.createExpectedPlaceholder(0, "# TODO", "# TODO")

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

}