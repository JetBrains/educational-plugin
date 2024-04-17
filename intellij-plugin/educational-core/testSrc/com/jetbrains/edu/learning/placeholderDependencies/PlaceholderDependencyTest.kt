package com.jetbrains.edu.learning.placeholderDependencies

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.withEduTestDialog
import org.junit.Test

class PlaceholderDependencyTest : NotificationsTestBase() {

  @Test
  fun `test placeholder replaced with solution`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved

    val virtualFile = findFileInTask(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task.kt", 1))
  }

  @Test
  fun `test unsolved dependency`() {
    courseWithFiles {
      lesson {
        eduTask { taskFile("task.txt", "task with <p>placeholder</p>") }
      }
      lesson {
        eduTask {
          taskFile("task.txt", "task with another <p>type here</p>") {
            placeholder(0, dependency = "lesson1#task1#task.txt#1")
          }
        }
      }
    }

    val virtualFile = findFileInTask(1, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)

    checkEditorNotification<UnsolvedDependenciesNotificationProvider>(virtualFile, UnsolvedDependenciesNotificationProvider.getText(listOf("task1")))

    checkPlaceholderContent("type here", findPlaceholder(1, 0, "task.txt", 0))
  }

  @Test
  fun `test no replacement`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Failed

    val virtualFile = findFileInTask(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("type here", findPlaceholder(1, 0, "Task.kt", 1))
  }

  @Test
  fun `test several files`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def foo():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(0, dependency = "lesson1#task1#Task.kt#1")
          }
          taskFile("Task1.kt", """
            |def bar():
            |<p>type here</p>
          """.trimMargin("|")) {
            placeholder(0, dependency = "lesson1#task1#Task.kt#1")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved

    val virtualFile = findFileInTask(1, 0, "Task.kt")
    myFixture.openFileInEditor(virtualFile)

    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task.kt", 0))
    checkPlaceholderContent("print(1)", findPlaceholder(1, 0, "Task1.kt", 0))
  }

  @Test
  fun `test refresh task file with dependency`() {
    courseWithFiles {
      lesson {
        eduTask { taskFile("task.txt", "<p>placeholder</p>") }
      }
      lesson {
        eduTask {
          taskFile("task.txt", "this is a <p>type here</p> and one more <p>type here</p>") {
            placeholder(0, dependency = "lesson1#task1#task.txt#1")
            placeholder(0, possibleAnswer = "placeholder")
          }
        }
      }
    }

    getCourse().lessons[0].taskList[0].status = CheckStatus.Solved
    val secondPlaceholder = findPlaceholder(1, 0, "task.txt", 1)
    CCUtils.replaceAnswerPlaceholder(secondPlaceholder.taskFile.getDocument(project)!!, secondPlaceholder)

    val virtualFile = findFileInTask(1, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)

    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    checkPlaceholderContent("placeholder", findPlaceholder(1, 0, "task.txt", 0))
    checkPlaceholderContent("type here", findPlaceholder(1, 0, "task.txt", 1))
  }

  private fun checkPlaceholderContent(expectedContent: String, answerPlaceholder: AnswerPlaceholder) {
    val taskFile = answerPlaceholder.taskFile
    val document = taskFile.getDocument(project)!!
    val startOffset = answerPlaceholder.offset
    val endOffset = answerPlaceholder.endOffset
    val actualContent = document.getText(TextRange.create(startOffset, endOffset))

    assertEquals("Placeholder content is incorrect", expectedContent, actualContent)
  }
}