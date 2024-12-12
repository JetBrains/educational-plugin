package com.jetbrains.edu.cognifire.highlighting.prompttocode

import com.intellij.openapi.editor.event.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.LinkingHighlighter
import com.jetbrains.edu.cognifire.highlighting.highlighers.UncommitedChangesHighlighter
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.learning.selectedEditor

/**
 * Class [PromptToCodeHighlighter] is responsible for highlighting the prompt and code lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property highlighterManager The instance of [HighlighterManager].
 * @property listenerManager The instance of [ListenerManager].
 *
 */
class PromptToCodeHighlighter(private val project: Project, private val actionId: String) {

  private val highlighterManager = HighlighterManager.getInstance()
  private val listenerManager = ListenerManager.getInstance(project)

  /**
   * Sets up the EditorMouseMotionListener to handle mouse movement events in the editor.
   * The method creates an anonymous class implementation of the EditorMouseMotionListener interface
   *  and registers it with the ListenerManager. The listener handles the logic for highlighting lines of code based on the mouse position.
   *
   * @param promptExpression Represents the `prompt` block.
   * @param codeExpression Represents the `code` block.
   * @param promptToCodeLines A map that contains the line numbers in the prompt section as the keys
   * and a list of corresponding line numbers in the code section as the values.
   * @param codeToPromptLines A map that contains the line numbers in the code section as the keys
   * and a list of corresponding line numbers in the prompt section as the values.
   */
  fun setUp(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>
  ) {
    listenerManager.addListener(
      getMouseMotionListener(
        promptExpression,
        codeExpression,
        promptToCodeLines,
        codeToPromptLines
      ),
      actionId
    )
    listenerManager.addListener(
      getDocumentListener(codeExpression, promptExpression),
      actionId
    )
  }

  fun setUpDocumentListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression
  ) {
    listenerManager.addListener(
      getDocumentListener(codeExpression, promptExpression),
      actionId
    )
  }

  private fun getMouseMotionListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      val editor = e.editor
      val selectedLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line

      val promptLineOffset = editor.document.getLineNumber(promptExpression.contentOffset)
      val codeLineOffset = editor.document.getLineNumber(codeExpression.contentOffset)

      highlighterManager.clearProdeHighlighters<LinkingHighlighter>(actionId)

      if(selectedLineWithOffset - promptLineOffset in promptToCodeLines.keys) showHighlighters(
        selectedLineWithOffset - promptLineOffset,
        promptLineOffset,
        codeLineOffset,
        promptToCodeLines,
        codeToPromptLines
      )
      else if (selectedLineWithOffset - codeLineOffset in codeToPromptLines.keys) showHighlighters(
        selectedLineWithOffset - codeLineOffset,
        codeLineOffset,
        promptLineOffset,
        codeToPromptLines,
        promptToCodeLines
      )
    }
  }

  private fun getDocumentListener(codeExpression: CodeExpression, promptExpression: PromptExpression) = object: DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      if (project.isDisposed) return
      val delta = event.newLength - event.oldLength

      if (event.offset < promptExpression.startOffset) {
        promptExpression.shiftStartOffset(delta)
      }
      if (event.offset < promptExpression.endOffset){
        promptExpression.shiftEndOffset(delta)
      }
      if (event.offset < codeExpression.startOffset) {
        codeExpression.shiftStartOffset(delta)
      }
      if (event.offset < codeExpression.endOffset) {
        codeExpression.shiftEndOffset(delta)
      }

      if (event.offset in promptExpression.startOffset until promptExpression.endOffset ||
          event.offset in codeExpression.startOffset until codeExpression.endOffset) {
        highlighterManager.clearProdeHighlighters<LinkingHighlighter>(actionId)
        listenerManager.clearAllMouseMotionListeners(actionId)

        if (delta > 0) {
          val editor = project.selectedEditor ?: error("No editor selected")
          highlighterManager.addProdeHighlighter(
            UncommitedChangesHighlighter(editor, event.offset, event.offset + delta),
            actionId
          )
        }
        if (delta != 0) {
          addReadOnlyBlock(codeExpression, promptExpression, event)
        }
      }
    }
  }

  private fun addReadOnlyBlock(codeExpression: CodeExpression, promptExpression: PromptExpression, event: DocumentEvent) {
    val document = event.document
    val guardManager = GuardedBlockManager.getInstance()
    if (event.offset in promptExpression.startOffset until promptExpression.endOffset) {
      guardManager.addGuardedBlock(document, codeExpression.startOffset, codeExpression.endOffset, actionId, true)
    } else if (event.offset in codeExpression.startOffset until codeExpression.endOffset) {
      guardManager.addGuardedBlock(document, promptExpression.startOffset, promptExpression.endOffset, actionId, false)
    }
  }

  private fun showHighlighters(
    originLine: Int,
    originLineOffset: Int,
    destinationLineOffset: Int,
    originToDestination: Map<Int, List<Int>>,
    destinationToOrigin: Map<Int, List<Int>>,
  ) {
    val destinationLines = originToDestination[originLine] ?: return
    val destinationLinesWithOffset = destinationLines.map { it + destinationLineOffset }
    val originLines = destinationToOrigin[destinationLines.singleOrNull()] ?: listOf(originLine)
    val originLinesWithOffset = originLines.map { it + originLineOffset }

    addHighlighters(destinationLinesWithOffset + originLinesWithOffset)
  }

  private fun addHighlighters(lines: List<Int>) = try {
    lines.forEach { line ->
      highlighterManager.addProdeHighlighter(
        LinkingHighlighter(project, line),
        actionId
      )
    }
  }
  catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }
}
