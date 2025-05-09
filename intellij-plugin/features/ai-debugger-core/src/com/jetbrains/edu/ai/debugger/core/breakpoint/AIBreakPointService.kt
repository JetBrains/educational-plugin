package com.jetbrains.edu.ai.debugger.core.breakpoint

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.ui.DebuggerColors
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.getEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Color

@Service(Service.Level.PROJECT)
class AIBreakPointService(private val project: Project, private val scope: CoroutineScope) {

  private val highlighterRangers = mutableMapOf<XBreakpoint<out XBreakpointProperties<*>>, RangeHighlighter>()

  private val breakpointManager: XBreakpointManager
    get() = XDebuggerManager.getInstance(project).breakpointManager

  fun initialize() {
    val language = project.course?.languageById ?: return
    val type = language.getAIBreakpointType()
    breakpointManager.getBreakpoints(type).forEach(::highlightBreakpoint)
    addListener(type)
  }


  fun toggleLineBreakpoint(language: Language, file: VirtualFile, line: Int) = runReadAction {
    val type = language.getAIBreakpointType()
    removeBreakpointsOnLine(line)
    breakpointManager.addLineBreakpoint(type, file.url, line, type.createProperties())
  }

  private fun removeBreakpointsOnLine(line: Int) {
    breakpointManager.allBreakpoints.filter { it.sourcePosition?.line == line }.forEach { breakpointManager.removeBreakpoint(it) }
  }

  private val listener by lazy {
    object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
      override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        super.breakpointAdded(breakpoint)
        highlightBreakpoint(breakpoint)
      }

      override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
        removeHighlighter(breakpoint)
        super.breakpointRemoved(breakpoint)
      }
    }
  }

  private fun highlightBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) = scope.launch(Dispatchers.IO) {
    val position = breakpoint.sourcePosition ?: error("There is no position for the breakpoint")
    val editor = position.file.getEditor(project) ?: return@launch
    val attribute = TextAttributes().apply {
      backgroundColor = HIGHLIGHTING_COLOR
    }
    val range = editor.markupModel.addLineHighlighter(
      position.line,
      DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER + 1,
      attribute
    )
    highlighterRangers[breakpoint] = range
  }

  fun removeHighlighter(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
    val position = breakpoint.sourcePosition ?: error("There are no position for the breakpoint")
    val range = highlighterRangers[breakpoint]
    range?.let {
      position.file.getEditor(project)?.markupModel?.removeHighlighter(it)
    }
  }

  private fun addListener(type: XLineBreakpointType<XBreakpointProperties<*>>) {
    breakpointManager.addBreakpointListener(type, listener)
  }

  companion object {
    private val HIGHLIGHTING_COLOR: Color = JBColor(Color(109, 0, 247, 77), Color(135, 49, 247, 77))

    fun Language.getAIBreakpointType(): XLineBreakpointType<XBreakpointProperties<*>> =
      XDebuggerUtil
        .getInstance()
        .findBreakpointType(BreakpointTypeManager.getInstance(this).getBreakPointType()::class.java)
  }
}
