package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

class CCAddAnswerPlaceholderActionTest : AnswerPlaceholderTestBase() {

  fun `test add placeholder without selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", defaultTaskText)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val placeholderExpected = AnswerPlaceholder()
    placeholderExpected.offset = 0
    placeholderExpected.length = 9
    placeholderExpected.index = 0
    placeholderExpected.taskFile = taskFileExpected
    placeholderExpected.possibleAnswer = defaultPlaceholderText
    placeholderExpected.placeholderText = defaultPlaceholderText
    taskFileExpected.answerPlaceholders.add(placeholderExpected)

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

  fun `test add placeholder with selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", defaultTaskText)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val placeholderExpected = AnswerPlaceholder()
    placeholderExpected.offset = 10
    placeholderExpected.length = 9
    placeholderExpected.index = 0
    placeholderExpected.taskFile = taskFileExpected
    placeholderExpected.possibleAnswer = defaultTaskText.substring(10, 19)
    placeholderExpected.placeholderText = defaultPlaceholderText
    taskFileExpected.answerPlaceholders.add(placeholderExpected)

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

  fun `test placeholder intersection`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", defaultTaskText)
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
          taskFile("Task.kt", defaultTaskText)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val placeholderExpected = AnswerPlaceholder()
    placeholderExpected.offset = 0
    placeholderExpected.length = 9
    placeholderExpected.index = 0
    placeholderExpected.taskFile = taskFileExpected
    placeholderExpected.possibleAnswer = defaultPlaceholderText
    placeholderExpected.placeholderText = defaultPlaceholderText
    val placeholderDependency = AnswerPlaceholderDependency(placeholderExpected, null, "lesson1", "task1", "Task.kt", 1, true)
    placeholderExpected.placeholderDependency = placeholderDependency
    taskFileExpected.answerPlaceholders.add(placeholderExpected)

    doTest("lesson1/task2/Task.kt",
           CCTestAddAnswerPlaceholder(CCCreateAnswerPlaceholderDialog.DependencyInfo("lesson1#task1#Task.kt#1", true)), taskFile,
           taskFileExpected)
  }

  private class CCTestAddAnswerPlaceholder(val dependencyInfo: CCCreateAnswerPlaceholderDialog.DependencyInfo? = null) : CCAddAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      return object : CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getTaskText(): String = "type here"
        override fun getDependencyInfo(): DependencyInfo? = dependencyInfo
      }
    }
  }
}