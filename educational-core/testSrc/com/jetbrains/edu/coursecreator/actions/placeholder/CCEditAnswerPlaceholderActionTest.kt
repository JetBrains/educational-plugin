package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class CCEditAnswerPlaceholderActionTest : AnswerPlaceholderTestBase() {
  fun `test add placeholder with dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>")
        }
        eduTask("task2") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>")
        }
      }
    }

    val taskFile = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!
    val firstTask = course.lessons[0].taskList[0]!!

    doTest("lesson1/task2/Task.kt", CCTestEditAnswerPlaceholder(), taskFile, 20, 20, firstTask)
  }

  private class CCTestEditAnswerPlaceholder : CCEditAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      val placeholderText = answerPlaceholder.placeholderText
      return object : CCCreateAnswerPlaceholderDialog(project, placeholderText ?: "type here", false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getTaskText(): String = "type here"
        override fun getDependencyInfo(): DependencyInfo? = DependencyInfo("lesson1#task1#Task.kt#1", true)
      }
    }
  }
}