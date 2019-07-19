package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.coursecreator.CCTestsUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler

class CCAnswerPlaceholderActionTest : CCTestCase() {

  fun testPlaceholderWithSelection() = doTest("onePlaceholder", CCTestAction())
  fun testPlaceholderWithoutSelection() = doTest("withoutSelection", CCTestAction())

  fun testPlaceholderIntersection() {
    configureByTaskFile("placeholderIntersection.txt")
    val presentation = myFixture.testAction(CCTestAction())
    assertTrue(presentation.isVisible && !presentation.isEnabled)
  }

  fun testPlaceholderDeleted() = doTest("deletePlaceholder", CCDeleteAnswerPlaceholder())

  private fun doTest(name: String, action: AnAction) {
    val virtualFile = configureByTaskFile(name + CCTestsUtil.BEFORE_POSTFIX)
    myFixture.testAction(action)
    val taskFile = EduUtils.getTaskFile(project, virtualFile) ?: error("Failed to find task file for $virtualFile")
    setPossibleAnswers(taskFile)
    checkByFile(taskFile, name + CCTestsUtil.AFTER_POSTFIX, false)
    checkPainters(taskFile)
    if (action is CCAddAnswerPlaceholder) {
      val document = myFixture.getDocument(myFixture.file)
      val handler = EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document)
      assertInstanceOf(handler, AnswerPlaceholderDeleteHandler::class.java)
    }
    UndoManager.getInstance(project).undo(FileEditorManager.getInstance(project).getSelectedEditor(virtualFile))
    checkByFile(taskFile, name + CCTestsUtil.BEFORE_POSTFIX, false)
  }

  private fun setPossibleAnswers(taskFile: TaskFile) {
    for (placeholder in taskFile.answerPlaceholders) {
      placeholder.possibleAnswer = taskFile.getDocument(project)?.getText(TextRange.create(placeholder.offset, placeholder.endOffset))
    }
  }

  override fun getBasePath(): String = super.getBasePath() + "/actions/addPlaceholder"

  private class CCTestAction : CCAddAnswerPlaceholder() {
    override fun createDialog(project: Project, answerPlaceholder: AnswerPlaceholder): CCCreateAnswerPlaceholderDialog {
      val placeholderText = answerPlaceholder.placeholderText
      return object : CCCreateAnswerPlaceholderDialog(project, placeholderText ?: "type here", false) {
        override fun showAndGet(): Boolean = true
        override fun getTaskText(): String = "type here"
      }
    }
  }
}
