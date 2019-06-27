package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency

class CCEditAnswerPlaceholderActionTest : CCAnswerPlaceholderTestBase() {
  fun `test add placeholder with dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>")
        }
        eduTask("task2") {
          taskFile("Task.kt", "<p>fun foo()</p>: String = TODO()")
        }
      }
    }

    val taskFile = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    val placeholderExpected = taskFileExpected.answerPlaceholders[0]
    placeholderExpected.placeholderText = defaultPlaceholderText
    placeholderExpected.placeholderDependency = AnswerPlaceholderDependency(placeholderExpected, null, "lesson1", "task1", "Task.kt", 1,
                                                                            true)
    placeholderExpected.taskFile = taskFileExpected
    doTest("lesson1/task2/Task.kt", CCTestEditAnswerPlaceholder(), taskFile, taskFileExpected)
  }

  private class CCTestEditAnswerPlaceholder : CCEditAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      return object : CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getPlaceholderText(): String = "type here"
        override fun getDependencyInfo(): DependencyInfo? = DependencyInfo("lesson1#task1#Task.kt#1", true)
      }
    }
  }
}