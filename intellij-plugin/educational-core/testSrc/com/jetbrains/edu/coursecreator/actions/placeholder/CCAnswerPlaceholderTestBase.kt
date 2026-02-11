package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.CCTestsUtil.checkPainters
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler
import com.jetbrains.edu.learning.testAction

abstract class CCAnswerPlaceholderTestBase : EduActionTestCase() {
  companion object {
    const val DEFAULT_TASK_TEXT = "fun foo(): String = TODO()"
  }

  override fun tearDown() {
    try {
      // some events can be still in edt (such as yaml sync) and we need them to finish before project is disposed
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  fun doTest(
    name: String,
    action: CCAnswerPlaceholderAction,
    taskFile: TaskFile,
    taskFileExpected: TaskFile,
    selection: CCAddAnswerPlaceholderActionTestBase.Selection? = null
  ) {
    val taskFileUnchanged = copy(taskFile)
    val virtualFile = findFile(name)
    myFixture.openFileInEditor(virtualFile)
    if (selection != null) {
      myFixture.editor.selectionModel.setSelection(selection.start, selection.end)
    }
    testAction(action)

    checkPlaceholders(taskFileExpected, taskFile)
    checkPainters(taskFile)

    if (action is CCAddAnswerPlaceholder) {
      val document = myFixture.getDocument(myFixture.file)
      val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
      assertInstanceOf(handler, AnswerPlaceholderDeleteHandler::class.java)
    }

    UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(virtualFile))
    assertEquals(taskFileUnchanged.name, taskFile.name)
    assertEquals(taskFileUnchanged.text, taskFile.text)
    assertEquals(taskFileUnchanged.answerPlaceholders.size, taskFile.answerPlaceholders.size)
    assertEquals(taskFileUnchanged.task, taskFile.task)
    checkPlaceholders(taskFileUnchanged, taskFile)
  }

  private fun checkPlaceholders(taskFileExpected: TaskFile, taskFileActual: TaskFile) {
    val placeholdersActual = taskFileActual.answerPlaceholders
    val placeholdersExpected = taskFileExpected.answerPlaceholders
    assertEquals(placeholdersExpected.size, placeholdersActual.size)
    placeholdersExpected.forEachIndexed { i, placeholderExpected ->
      run {
        val placeholderActual = placeholdersActual[i]
        assertNotNull(placeholderActual)
        assertEquals(placeholderExpected.offset, placeholderActual.offset)
        assertEquals(placeholderExpected.length, placeholderActual.length)
        assertEquals(placeholderExpected.index, placeholderActual.index)
        assertEquals(placeholderExpected.placeholderText, placeholderActual.placeholderText)
        assertEquals(placeholderExpected.taskFile.text, placeholderActual.taskFile.text)
        assertEquals(placeholderExpected.taskFile.name, placeholderActual.taskFile.name)

        val expectedDependency = placeholderExpected.placeholderDependency
        if (expectedDependency == null) {
          assertNull(placeholderActual.placeholderDependency)
        }
        else {
          val actualDependency = placeholderActual.placeholderDependency ?: error("Answer placeholder dependency should not be null")
          assertEquals(expectedDependency.fileName, actualDependency.fileName)
          assertEquals(expectedDependency.taskName, actualDependency.taskName)
          assertEquals(expectedDependency.lessonName, actualDependency.lessonName)
          assertEquals(expectedDependency.isVisible, actualDependency.isVisible)
        }
      }
    }
  }

  fun copy(source: TaskFile): TaskFile {
    val target = TaskFile()
    val sourceAnswerPlaceholders = source.answerPlaceholders
    val answerPlaceholdersCopy = ArrayList<AnswerPlaceholder>(sourceAnswerPlaceholders.size)
    for (answerPlaceholder in sourceAnswerPlaceholders) {
      val answerPlaceholderCopy = AnswerPlaceholder()
      answerPlaceholderCopy.placeholderText = answerPlaceholder.placeholderText
      answerPlaceholderCopy.offset = answerPlaceholder.offset
      answerPlaceholderCopy.length = answerPlaceholder.length
      answerPlaceholderCopy.index = answerPlaceholder.index
      val state = answerPlaceholder.initialState
      answerPlaceholderCopy.initialState = AnswerPlaceholder.MyInitialState(state.offset, state.length)
      answerPlaceholdersCopy.add(answerPlaceholderCopy)
    }
    target.name = source.name
    target.answerPlaceholders = answerPlaceholdersCopy

    target.text = source.text
    target.task = source.task
    source.answerPlaceholders.forEachIndexed { i, taskFilePlaceholder ->
      run {
        target.answerPlaceholders[i].taskFile = taskFilePlaceholder.taskFile
      }
    }
    return target
  }
}