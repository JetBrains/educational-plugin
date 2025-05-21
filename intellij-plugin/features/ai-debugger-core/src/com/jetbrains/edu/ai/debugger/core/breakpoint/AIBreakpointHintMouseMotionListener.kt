package com.jetbrains.edu.ai.debugger.core.breakpoint

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakPointService.Companion.getAIBreakpointType
import com.jetbrains.edu.ai.debugger.core.ui.AIBreakpointHint
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails


class AIBreakpointHintMouseMotionListener(
  private val breakpointHints: List<BreakpointHintDetails>,
) : EditorMouseMotionListener, EditorMouseListener {

  private var breakpointHint: AIBreakpointHint? = null
  private var currentLine: Int? = null

  override fun mouseMoved(e: EditorMouseEvent) {
    val editor = e.editor
    val line = EditorUtil.yToLogicalLineNoCustomRenderers(editor, e.mouseEvent.y)
    if (line == currentLine && breakpointHint != null) return
    currentLine = line
    breakpointHint?.close()
    if (e.area != EditorMouseEventArea.LINE_NUMBERS_AREA) return
    val virtualFile = editor.virtualFile ?: return
    if (!hasBreakpointAtLine(editor, line, virtualFile)) return
    val fileName = virtualFile.name
    val message = breakpointHints.getHint(line, fileName) ?: error("No breakpoint hint is found")
    breakpointHint = AIBreakpointHint(message, editor, getTextStartOffset(editor, line))
  }

  private fun hasBreakpointAtLine(editor: Editor, line: Int, virtualFile: VirtualFile): Boolean {
    val project = editor.project ?: return false
    val language = project.course?.languageById ?: return false
    val type = language.getAIBreakpointType()
    return XDebuggerManager.getInstance(project).breakpointManager.findBreakpointsAtLine(type, virtualFile, line).isNotEmpty()
  }

  override fun mouseExited(event: EditorMouseEvent) {
    breakpointHint?.close()
    breakpointHint = null
    currentLine = null
  }

  private fun List<BreakpointHintDetails>.getHint(line: Int, fileName: String): String? = firstOrNull {
    it.fileName.substringAfterLast("/") == fileName && it.lineNumber == line
  }?.hint

  private fun getTextStartOffset(editor: Editor, line: Int): Int {
    val document = editor.document
    val lineStartOffset = document.getLineStartOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(line)))
    val contentStartOffset = lineText.indexOfFirst { !it.isWhitespace() }.takeIf { it != -1 } ?: 0
    return lineStartOffset + contentStartOffset
  }
}