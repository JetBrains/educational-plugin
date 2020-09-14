package com.jetbrains.edu.cpp.actions

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.placeholder.CCAddAnswerPlaceholderActionTestBase

class CppCatchAddAnswerPlaceholderActionTest : CCAddAnswerPlaceholderActionTestBase() {

  fun `test placeholder text`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = OCLanguage.getInstance(),
                                 environment = "Catch") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.createExpectedPlaceholder(0, "/* TODO */", "/* TODO */")

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

}