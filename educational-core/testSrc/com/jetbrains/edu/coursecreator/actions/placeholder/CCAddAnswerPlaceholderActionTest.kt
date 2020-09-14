package com.jetbrains.edu.coursecreator.actions.placeholder

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.DEFAULT_PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

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

  fun `test add placeholder with dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun <p>foo(): String = TODO()</p>")
        }
        eduTask("task2") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val placeholderExpected = taskFileExpected.createExpectedPlaceholder(0, DEFAULT_PLACEHOLDER_TEXT, DEFAULT_PLACEHOLDER_TEXT)
    val placeholderDependency = AnswerPlaceholderDependency(placeholderExpected, null, "lesson1", "task1", "Task.kt", 1, true)
    placeholderExpected.placeholderDependency = placeholderDependency
    doTest("lesson1/task2/Task.kt",
           CCTestAddAnswerPlaceholder(CCCreateAnswerPlaceholderDialog.DependencyInfo("lesson1#task1#Task.kt#1", true)), taskFile,
           taskFileExpected)
  }
}
