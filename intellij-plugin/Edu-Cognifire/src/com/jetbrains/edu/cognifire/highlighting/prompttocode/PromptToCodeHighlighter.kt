package com.jetbrains.edu.cognifire.highlighting.prompttocode

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.LinkingHighlighter
import com.jetbrains.edu.cognifire.highlighting.highlighers.UncommittedChangesHighlighter
import com.jetbrains.edu.cognifire.highlighting.ProdeStateManager
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.selectedEditor
import kotlin.math.abs


const val EMPTY = ""

/**
 * Class [PromptToCodeHighlighter] is responsible for highlighting the prompt and code lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property highlighterManager The instance of [HighlighterManager].
 * @property listenerManager The instance of [ListenerManager].
 *
 */
class PromptToCodeHighlighter(private val project: Project, private val prodeId: String) {

  private val highlighterManager = HighlighterManager.getInstance()
  private val listenerManager = ListenerManager.getInstance(project)
  private val editor = project.selectedEditor
  private var initialPromptContent: String = EMPTY
  private var initialCodeContent: String = EMPTY
  private val prodeStateManager = ProdeStateManager.getInstance()

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
      prodeId
    )
    listenerManager.addListener(
      getDocumentListener(promptExpression, codeExpression),
      prodeId
    )
    initialPromptContent = getContent(editor?.document, promptExpression.startOffset, promptExpression.endOffset)
    initialCodeContent = getContent(editor?.document, codeExpression.startOffset, codeExpression.endOffset)

    prodeStateManager.clearAllListeners(prodeId)
    prodeStateManager.addProde(
      prodeId,
      promptToCodeLines,
      codeToPromptLines,
      initialPromptContent,
      initialCodeContent
    )
    prodeStateManager.addListener(prodeId, ProdeStateManager.ListenerType.DOCUMENT)
    prodeStateManager.addListener(prodeId, ProdeStateManager.ListenerType.MOUSE_MOTION)
  }

  fun subscribeListeners(promptExpression: PromptExpression, codeExpression: CodeExpression): Boolean {
    val listeners = prodeStateManager.listenerMetadata[prodeId] ?: return false
    val prodeData = prodeStateManager.prodeData[prodeId] ?: return false

    with(prodeData) {
      initialPromptContent = initialPrompt
      initialCodeContent = initialCode

      listeners.forEach { listener ->
        when (listener) {
          ProdeStateManager.ListenerType.MOUSE_MOTION -> {
            listenerManager.addListener(
              getMouseMotionListener(
                promptExpression,
                codeExpression,
                promptToCodeLines,
                codeToPromptLines
              ),
              prodeId
            )
          }

          ProdeStateManager.ListenerType.DOCUMENT -> {
            listenerManager.addListener(
              getDocumentListener(promptExpression, codeExpression),
              prodeId
            )
          }
        }
      }
    }
    return true
  }

  fun setUpDocumentListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression
  ) {
    initialPromptContent = getContent(editor?.document, promptExpression.startOffset, promptExpression.endOffset)
    initialCodeContent = getContent(editor?.document, codeExpression.startOffset, codeExpression.endOffset)
    listenerManager.addListener(
      getDocumentListener(promptExpression, codeExpression),
      prodeId
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

      highlighterManager.clearProdeHighlighters<LinkingHighlighter>(prodeId)

      if (selectedLineWithOffset - promptLineOffset in promptToCodeLines.keys) showHighlighters(
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

  private fun getDocumentListener(promptExpression: PromptExpression, codeExpression: CodeExpression) = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      if (project.isDisposed) return
      val delta = event.newLength - event.oldLength
      val offset = event.offset

      val eventRange = offset until offset + abs(delta)

      val promptRange = promptExpression.startOffset until promptExpression.endOffset
      val codeRange = codeExpression.startOffset until codeExpression.endOffset


      val prodeIsEdited = eventRange.intersect(promptRange).isNotEmpty() ||
                          eventRange.intersect(codeRange).isNotEmpty()


      if (prodeIsEdited) {
        clearHighlighters()
        if (delta > 0) handleUncommitedChanges(offset, delta)
        if (delta != 0) handleReadOnlyBlocks(codeExpression, promptExpression, event)
      }
    }
  }

  private fun clearHighlighters() {
    highlighterManager.clearProdeHighlighters<LinkingHighlighter>(prodeId)
    listenerManager.clearAllMouseMotionListeners(prodeId)
    prodeStateManager.clearAllListenersOfType(prodeId, ProdeStateManager.ListenerType.MOUSE_MOTION)
  }

  private fun handleUncommitedChanges(offset: Int, delta: Int) {
    highlighterManager.addProdeHighlighter(UncommittedChangesHighlighter(offset, offset + delta), prodeId, project)
  }

  private fun getContent(document: Document?, startOffset: Int, endOffset: Int): String {
    return document?.text?.getSubstringText(startOffset, endOffset)?.normalize() ?: EMPTY
  }

  private fun String.normalize() = this.replace(Regex("\\s+"), " ").trim()

  private fun String.getSubstringText(startOffset: Int, endOffset: Int) = try {
    this.substring(startOffset, endOffset)
    this.slice(startOffset until endOffset)
  }
  catch (e: StringIndexOutOfBoundsException) {
    EMPTY
  }

  private fun handleReadOnlyBlocks(codeExpression: CodeExpression, promptExpression: PromptExpression, event: DocumentEvent) {
    val document = event.document
    val guardManager = GuardedBlockManager.getInstance()
    val currentPromptContent = getContent(document, promptExpression.startOffset, codeExpression.startOffset)
    val currentCodeContent = getContent(document, codeExpression.startOffset, codeExpression.endOffset)

    if (event.offset in promptExpression.startOffset until promptExpression.endOffset) {
      if (currentPromptContent != initialPromptContent) {
        guardManager.addGuardedBlock(document, codeExpression.startOffset, codeExpression.endOffset, prodeId)
      }
      else {
        guardManager.removeGuardedBlock(prodeId, document)
      }
    }
    else if (event.offset in codeExpression.startOffset until codeExpression.endOffset) {
      if (currentCodeContent != initialCodeContent) {
        guardManager.addGuardedBlock(document, promptExpression.startOffset, promptExpression.endOffset, prodeId)
      }
      else {
        guardManager.removeGuardedBlock(prodeId, document)
      }
    }

    PsiDocumentManager.getInstance(project).commitDocument(document)
    editor?.contentComponent?.repaint()
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
      highlighterManager.addProdeHighlighter(LinkingHighlighter(line), prodeId, project)
    }
  }
  catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }
}
