package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder

class CCAddAnswerPlaceholderActionTest : AnswerPlaceholderTestBase() {

  fun `test add placeholder without selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile)
  }

  fun `test add placeholder with selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, 20, 26)
  }

  fun `test placeholder intersection`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
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
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    val taskFile = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!
    val firstTask = course.lessons[0].taskList[0]
    doTest("lesson1/task2/Task.kt",
           CCTestAddAnswerPlaceholder(CCCreateAnswerPlaceholderDialog.DependencyInfo("lesson1#task1#Task.kt#1", true)), taskFile, 20, 26,
           firstTask)
  }

  fun `test delete placeholder`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun <p>foo(): String = TODO()</p>")
        }
      }
    }

    val virtualFile = findFile("lesson1/task1/Task.kt")
    myFixture.openFileInEditor(virtualFile)
    myFixture.testAction(CCDeleteAnswerPlaceholder())
    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val answerPlaceholders = taskFile.answerPlaceholders
    assertNotNull(answerPlaceholders)
    assertEquals(0, answerPlaceholders.size)
    undoAddDependencyTest(virtualFile, 1, answerPlaceholders)
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