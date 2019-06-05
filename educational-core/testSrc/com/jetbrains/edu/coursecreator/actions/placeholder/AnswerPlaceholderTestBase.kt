package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler

abstract class AnswerPlaceholderTestBase : EduActionTestCase() {
  fun doTest(name: String,
             action: AnAction,
             taskFile: TaskFile,
             selectionStart: Int = 0,
             selectionEnd: Int = 0,
             task: Task? = null) {

    val virtualFile = findFile(name)
    myFixture.openFileInEditor(virtualFile)
    if (selectionStart != 0 && selectionEnd != 0) {
      myFixture.editor.selectionModel.setSelection(selectionStart, selectionEnd)
    }
    myFixture.testAction(action)

    val answerPlaceholders = taskFile.answerPlaceholders
    assertNotNull(answerPlaceholders)
    assertEquals(1, answerPlaceholders.size)
    val placeholder = answerPlaceholders[0]
    assertEquals("type here", placeholder.placeholderText)
    assertEquals(selectionStart, placeholder.offset)
    when {
      myFixture.editor.selectionModel.hasSelection() -> assertEquals(taskFile.text.subSequence(selectionStart, selectionEnd),
                                                                     placeholder.possibleAnswer)
      action is CCEditAnswerPlaceholder -> assertEquals("", placeholder.possibleAnswer)
      else -> assertEquals("type here", placeholder.possibleAnswer)
    }
    if (task != null) {
      val placeholderDependency = placeholder.placeholderDependency
      assertNotNull(placeholderDependency!!)
      assertEquals(task.lesson.name, placeholderDependency.lessonName)
      assertEquals(task.name, placeholderDependency.taskName)
      assertEquals("Task.kt", placeholderDependency.fileName)
      assertTrue(placeholderDependency.isVisible)
    }
    else {
      assertNull(placeholder.placeholderDependency)
    }

    if (action is CCAddAnswerPlaceholder) {
      val document = myFixture.getDocument(myFixture.file)
      val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
      assertInstanceOf(handler, AnswerPlaceholderDeleteHandler::class.java)
      undoAddDependencyTest(virtualFile, 0, answerPlaceholders)
    }
    else if (action is CCEditAnswerPlaceholder) {
      undoEditDependencyTest(virtualFile, answerPlaceholders)
    }
  }

  fun undoAddDependencyTest(virtualFile: VirtualFile, expectedPlaceholders: Int, answerPlaceholders: List<AnswerPlaceholder>) {
    UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(virtualFile))
    assertEquals(expectedPlaceholders, answerPlaceholders.size)
  }

  private fun undoEditDependencyTest(virtualFile: VirtualFile, answerPlaceholders: List<AnswerPlaceholder>) {
    UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(virtualFile))
    assertEquals(1, answerPlaceholders.size)
    assertEquals("TODO()", answerPlaceholders[0].possibleAnswer)
  }
}