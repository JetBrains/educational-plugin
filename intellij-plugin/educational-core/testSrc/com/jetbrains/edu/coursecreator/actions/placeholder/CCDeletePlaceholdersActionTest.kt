package com.jetbrains.edu.coursecreator.actions.placeholder

import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.getActionById
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class CCDeletePlaceholdersActionTest : CCAnswerPlaceholderTestBase() {

  @Test
  fun `test not available in student mode`() = doTestDeleteAll("Foo.kt", false, CCDeleteAllAnswerPlaceholdersAction.ACTION_ID,
                                                               CourseMode.STUDENT)
  @Test
  fun `test not available without placeholders`() = doTestDeleteAll("Bar.kt", false, CCDeleteAllAnswerPlaceholdersAction.ACTION_ID)
  @Test
  fun `test delete all placeholders`() = doTestDeleteAll("Foo.kt", true, CCDeleteAllAnswerPlaceholdersAction.ACTION_ID)

  @Test
  fun `test delete placeholder`() {
    val taskFileName = "Task.kt"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun <p>foo(): String = TODO()</p>")
        }
      }
    }
    val taskFile = course.findTask("lesson1", "task1").getTaskFile(taskFileName) ?: error("Failed to find `$taskFileName` task file")
    val taskFileExpected = copy(taskFile)
    taskFileExpected.answerPlaceholders = mutableListOf()

    val action = getActionById<CCDeleteAnswerPlaceholder>(CCDeleteAnswerPlaceholder.ACTION_ID)
    doTest("lesson1/task1/Task.kt", action, taskFile, taskFileExpected)
  }

  private fun doTestDeleteAll(
    taskFileName: String,
    shouldBeAvailable: Boolean,
    actionId: String,
    courseMode: CourseMode = CourseMode.EDUCATOR
  ) {
    val course = courseWithFiles(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Foo.kt", """fun <p>foo()</p>: String = <p>"Foo"</p>""")
          taskFile("Bar.kt", """fun bar(): String = "Bar"""")
        }
      }
    }
    val taskFile = course.findTask("lesson1", "task1").getTaskFile(taskFileName) ?: error("Failed to find `$taskFileName` task file")
    val file = taskFile.getVirtualFile(project) ?: error("Failed to find virtual files for `$taskFileName` task file")

    myFixture.configureFromExistingVirtualFile(file)

    testAction(actionId, shouldBeEnabled = shouldBeAvailable)
    if (shouldBeAvailable) {
      assertTrue("${CCDeleteAllAnswerPlaceholdersAction::class.java.simpleName} should delete all placeholdes",
                 taskFile.answerPlaceholders.isEmpty())
    }
  }
}
