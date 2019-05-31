package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import junit.framework.TestCase

class CCAddAnswerPlaceholderActionTest : EduActionTestCase() {

  fun `test add placeholder without selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    myFixture.openFileInEditor(findFile("lesson1/task1/Task.kt"))
    myFixture.testAction(CCTestAction(null))

    val answerPlaceholders = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!.answerPlaceholders
    TestCase.assertNotNull(answerPlaceholders)
    TestCase.assertEquals(1, answerPlaceholders.size)
    val placeholder = answerPlaceholders[0]
    TestCase.assertEquals("type here", placeholder.placeholderText)
    TestCase.assertEquals("type here", placeholder.possibleAnswer)
    TestCase.assertEquals(0, placeholder.offset)
    TestCase.assertNull(placeholder.placeholderDependency)
  }

  fun `test add placeholder with selection without dependency`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = TODO()")
        }
      }
    }

    myFixture.openFileInEditor(findFile("lesson1/task1/Task.kt"))
    myFixture.editor.selectionModel.setSelection(20, 26)
    myFixture.testAction(CCTestAction(null))

    val answerPlaceholders = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!.answerPlaceholders
    TestCase.assertNotNull(answerPlaceholders)
    TestCase.assertEquals(1, answerPlaceholders.size)
    val placeholder = answerPlaceholders[0]
    TestCase.assertEquals("type here", placeholder.placeholderText)
    TestCase.assertEquals("TODO()", placeholder.possibleAnswer)
    TestCase.assertEquals(20, placeholder.offset)
    TestCase.assertNull(placeholder.placeholderDependency)

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
    myFixture.testAction(CCTestAction(null))
    myFixture.editor.selectionModel.setSelection(0, 6)
    val presentation = myFixture.testAction(CCTestAction(null))
    CCTestCase.assertTrue(presentation.isVisible && !presentation.isEnabled)
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

    myFixture.openFileInEditor(findFile("lesson1/task2/Task.kt"))
    myFixture.editor.selectionModel.setSelection(20, 26)
    myFixture.testAction(CCTestAction(CCCreateAnswerPlaceholderDialog.DependencyInfo("lesson1#task1#Task.kt#1", true)))

    val answerPlaceholdersAdded = course.lessons[0].taskList[1].taskFiles["Task.kt"]!!.answerPlaceholders
    TestCase.assertNotNull(answerPlaceholdersAdded)
    TestCase.assertEquals(1, answerPlaceholdersAdded.size)
    val placeholderAdded = answerPlaceholdersAdded[0]
    TestCase.assertNotNull(placeholderAdded)
    TestCase.assertEquals("type here", placeholderAdded.placeholderText)
    TestCase.assertEquals("TODO()", placeholderAdded.possibleAnswer)
    TestCase.assertEquals(20, placeholderAdded.offset)
    val placeholderDependency = placeholderAdded.placeholderDependency!!
    TestCase.assertEquals("lesson1", placeholderDependency.lessonName)
    TestCase.assertEquals("task1", placeholderDependency.taskName)
    TestCase.assertEquals("Task.kt", placeholderDependency.fileName)
    TestCase.assertTrue(placeholderDependency.isVisible)
  }

  private class CCTestAction(val dependencyInfo: CCCreateAnswerPlaceholderDialog.DependencyInfo?) : CCAddAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      val placeholderText = answerPlaceholder.placeholderText
      return object : CCCreateAnswerPlaceholderDialog(project, placeholderText ?: "type here", false, answerPlaceholder) {
        override fun showAndGet(): Boolean = true
        override fun getTaskText(): String = "type here"
        override fun getDependencyInfo(): DependencyInfo? = dependencyInfo
      }
    }
  }
}