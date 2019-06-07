package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile

class CCDeletePlaceholdersActionTest : AnswerPlaceholderTestBase() {

  fun `test not available in student mode`() = doTest("Foo.kt", false, CCDeleteAllAnswerPlaceholdersAction(), EduNames.STUDY)
  fun `test not available without placeholders`() = doTest("Bar.kt", false, CCDeleteAllAnswerPlaceholdersAction())
  fun `test delete all placeholders`() = doTest("Foo.kt", true, CCDeleteAllAnswerPlaceholdersAction())
  fun `test delete placeholder`() = doTest("Foo.kt", true, CCDeleteAnswerPlaceholder())

  private fun doTest(taskFileName: String,
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
    val taskFileCopy = copy(taskFile)
    taskFileCopy.answerPlaceholders.forEach { it.taskFile = taskFileCopy }
    val file = taskFile.getVirtualFile(project) ?: error("Failed to find virtual files for `$taskFileName` task file")

    myFixture.configureFromExistingVirtualFile(file)

    val presentation = myFixture.testAction(action)

    assertEquals(shouldBeAvailable, presentation.isEnabledAndVisible)
    if (shouldBeAvailable) {
      val message = if (action is CCDeleteAllAnswerPlaceholdersAction) {
        "${CCDeleteAllAnswerPlaceholdersAction::class.java.simpleName} should delete all placeholders"
      }
      else {
        "${CCDeleteAnswerPlaceholder::class.java.simpleName} should delete placeholder"
      }
      assertTrue(message, taskFile.answerPlaceholders.isEmpty())
    }

    if (action is CCDeleteAnswerPlaceholder) {
      CCTestCase.checkPainters(taskFile)
      UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(file))
      checkPlaceholders(taskFileCopy, taskFile)
    }
  }
}
