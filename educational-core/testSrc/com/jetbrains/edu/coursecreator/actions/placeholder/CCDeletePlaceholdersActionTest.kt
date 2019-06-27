package com.jetbrains.edu.coursecreator.actions.placeholder

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile

class CCDeletePlaceholdersActionTest : CCAnswerPlaceholderTestBase() {

  fun `test not available in student mode`() = doTestDeleteAll("Foo.kt", false, CCDeleteAllAnswerPlaceholdersAction(), EduNames.STUDY)
  fun `test not available without placeholders`() = doTestDeleteAll("Bar.kt", false, CCDeleteAllAnswerPlaceholdersAction())
  fun `test delete all placeholders`() = doTestDeleteAll("Foo.kt", true, CCDeleteAllAnswerPlaceholdersAction())

  fun `test delete placeholder`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", "fun <p>foo(): String = TODO()</p>")
        }
      }
    }
    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.answerPlaceholders = emptyList()

    doTest("lesson1/task1/Task.kt", CCDeleteAnswerPlaceholder(), taskFile, taskFileExpected)
  }

  private fun doTestDeleteAll(taskFileName: String,
                              shouldBeAvailable: Boolean,
                              action: CCAnswerPlaceholderAction,
                              courseMode: String = CCUtils.COURSE_MODE) {
    val course = courseWithFiles(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Foo.kt", """fun foo(): String = <p>"Foo"</p>""")
          taskFile("Bar.kt", """fun bar(): String = "Bar"""")
        }
      }
    }
    val taskFile = course.findTask("lesson1", "task1").getTaskFile(taskFileName) ?: error("Failed to find `$taskFileName` task file")
    val file = taskFile.getVirtualFile(project) ?: error("Failed to find virtual files for `$taskFileName` task file")

    myFixture.configureFromExistingVirtualFile(file)

    val presentation = myFixture.testAction(action)

    assertEquals(shouldBeAvailable, presentation.isEnabledAndVisible)
    if (shouldBeAvailable) {
      assertTrue("${CCDeleteAllAnswerPlaceholdersAction::class.java.simpleName} should delete all placeholdes",
                 taskFile.answerPlaceholders.isEmpty())
    }
  }
}
