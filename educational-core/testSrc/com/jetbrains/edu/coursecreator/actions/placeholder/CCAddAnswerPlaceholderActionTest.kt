package com.jetbrains.edu.coursecreator.actions.placeholder

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.DEFAULT_PLACEHOLDER_TEXT

class CCAddAnswerPlaceholderActionTest : CCAddAnswerPlaceholderActionTestBase() {

  fun `test add placeholder without selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.createExpectedPlaceholder(0, DEFAULT_PLACEHOLDER_TEXT, DEFAULT_PLACEHOLDER_TEXT)

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

  fun `test add placeholder with selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val selection = Selection(10, 19)
    taskFileExpected.createExpectedPlaceholder(10, DEFAULT_PLACEHOLDER_TEXT, DEFAULT_TASK_TEXT.substring(10, 19))

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected, selection)
  }

  fun `test placeholder intersection`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    myFixture.openFileInEditor(findFile("lesson1/task1/Task.kt"))
    myFixture.testAction(CCTestAddAnswerPlaceholder())
    myFixture.editor.selectionModel.setSelection(0, 6)
    val presentation = myFixture.testAction(CCTestAddAnswerPlaceholder())
    assertTrue(presentation.isVisible && !presentation.isEnabled)
  }

  fun `test add placeholder action is disabled in non templated based framework lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      frameworkLesson("lesson1", isTemplateBased = false) {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    val file = findFile("lesson1/task1/Task.kt")
    myFixture.openFileInEditor(file)
    val presentation = myFixture.testAction(CCTestAddAnswerPlaceholder())
    checkActionEnabled(presentation, false)
  }
}
