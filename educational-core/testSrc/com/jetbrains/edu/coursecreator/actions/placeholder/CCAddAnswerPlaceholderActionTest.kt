package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.TaskFile

class CCAddAnswerPlaceholderActionTest : CCAnswerPlaceholderTestBase() {

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
    taskFileExpected.createExpectedPlaceholder()

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
    val selection = Selection(10, 19)
    taskFileExpected.createExpectedPlaceholder(selection)

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected, selection)
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
    val placeholderExpected = taskFileExpected.createExpectedPlaceholder()
    val placeholderDependency = AnswerPlaceholderDependency(placeholderExpected, null, "lesson1", "task1", "Task.kt", 1, true)
    placeholderExpected.placeholderDependency = placeholderDependency
    doTest("lesson1/task2/Task.kt",
           CCTestAddAnswerPlaceholder(CCCreateAnswerPlaceholderDialog.DependencyInfo("lesson1#task1#Task.kt#1", true)), taskFile,
           taskFileExpected)
  }

  private class CCTestAddAnswerPlaceholder(val dependencyInfo: CCCreateAnswerPlaceholderDialog.DependencyInfo? = null) : CCAddAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      return object : CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getPlaceholderText(): String = "type here"
        override fun getDependencyInfo(): DependencyInfo? = dependencyInfo
      }
    }
  }

  data class Selection(val start: Int, val end: Int) {
    val length = end - start
  }

  private fun TaskFile.createExpectedPlaceholder(selection: Selection? = null): AnswerPlaceholder {
    val placeholderExpected = AnswerPlaceholder()
    placeholderExpected.offset = selection?.start ?: 0
    placeholderExpected.length = selection?.length ?: 9
    placeholderExpected.index = 0
    placeholderExpected.taskFile = this
    placeholderExpected.possibleAnswer = if (selection == null) defaultPlaceholderText
    else defaultTaskText.substring(selection.start, selection.end)
    placeholderExpected.placeholderText = defaultPlaceholderText
    this.answerPlaceholders.add(placeholderExpected)
    return placeholderExpected
  }
}