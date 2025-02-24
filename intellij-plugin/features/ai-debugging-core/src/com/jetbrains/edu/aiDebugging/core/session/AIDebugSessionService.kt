package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withModalProgress
import com.jetbrains.edu.aiDebugging.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.aiDebugging.core.breakpoint.IntermediateBreakpointProcessor
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.ai.debugger.prompt.core.FixCodeForTestAssistant
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.FixCodeForTestResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.jetbrains.educational.ml.ai.debugger.prompt.core.BreakpointHintAssistant
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.breakpoint.FinalBreakpoint
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.breakpoint.IntermediateBreakpoint
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.BreakpointHintResponse

@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  fun runDebuggingSession(
    task: Task,
    description: TaskDescription,
    virtualFiles: List<VirtualFile>,
    testResult: CheckResult,
    closeAIDebuggingHint: () -> Unit
  ) {
    coroutineScope.launch {
      withModalProgress(project, EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
        FixCodeForTestAssistant.getCodeFix(
          description,
          virtualFiles.toNumberedLineMap(),
          testResult.details ?: testResult.message
        )
      }.onSuccess { fixes ->
        val language = project.course?.languageById ?: error("Language is not found")
        val intermediateBreakpoints = calculateIntermediateBreakpointPositions(fixes, virtualFiles, language)
        fixes.groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
          fileName to fixesForFile.map { it.wrongCodeLineNumber }
        }.toMap().toggleLineBreakpoint(virtualFiles, language)
        intermediateBreakpoints.toggleLineBreakpoint(virtualFiles, language)
        val breakpointHints = generateIntermediateBreakpointHints(virtualFiles, fixes, intermediateBreakpoints) ?: run {
          EduNotificationManager.showErrorNotification(project, content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail"))
          return@onSuccess
        }
        AIDebugSessionRunner(project, task, closeAIDebuggingHint).runDebuggingSession(testResult, fixes, breakpointHints)
      }.onFailure {
        EduNotificationManager.showErrorNotification(
          project,
          content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
        )
      }
    }
  }

  private fun calculateIntermediateBreakpointPositions(fixes: FixCodeForTestResponse, virtualFiles: List<VirtualFile>, language: Language) =
    fixes.groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
      fileName to runReadAction {
        IntermediateBreakpointProcessor.calculateIntermediateBreakpointPositions(
          virtualFiles.getVirtualFile(fileName),
          fixesForFile.map { it.wrongCodeLineNumber }.toList(),
          project,
          language
        )
      }
    }.toMap()

  private fun Map<String, List<Int>>.toggleLineBreakpoint(virtualFiles: List<VirtualFile>, language: Language) {
    forEach { (fileName, positions) ->
      positions.forEach { position ->
        project.getService(AIBreakPointService::class.java)
          .toggleLineBreakpoint(language, virtualFiles.getVirtualFile(fileName), position)
      }
    }
  }

  private suspend fun generateIntermediateBreakpointHints(
    virtualFiles: List<VirtualFile>,
    fixes: FixCodeForTestResponse,
    intermediateBreakpointPositions: Map<String, List<Int>>
  ): BreakpointHintResponse? {
    val finalBreakpoints = fixes.map {
      val line = virtualFiles.getVirtualFile(it.fileName).getLine(it.wrongCodeLineNumber)
      FinalBreakpoint(it.fileName, it.wrongCodeLineNumber, line, it.breakpointHint)
    }
    val intermediateBreakpoint = intermediateBreakpointPositions.map { (fileName, positions) ->
      positions.map { position ->
        val line = virtualFiles.getVirtualFile(fileName).getLine(position)
        IntermediateBreakpoint(fileName, position, line)
      }
    }.flatten()
    return withModalProgress(project, EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
      BreakpointHintAssistant.getBreakpointHints(virtualFiles.toNumberedLineMap(), finalBreakpoints, intermediateBreakpoint)
    }.getOrNull()
  }

  private fun List<VirtualFile>.getVirtualFile(fileName: String) = firstOrNull { file -> file.name == fileName }
                                                                   ?: error("Virtual file is not found")

  private fun VirtualFile.getLine(line: Int): String = runReadAction {
    val document = FileDocumentManager.getInstance().getDocument(this) ?: error("Document is not found")
    if (line < 0 || line >= document.lineCount) error("Line number $line is out of bounds")
    document.text.substring(document.getLineStartOffset(line), document.getLineEndOffset(line)).trim()
  }

  private fun List<VirtualFile>.toNumberedLineMap() = runReadAction {
    associate { it.name to it.document.text.lines().mapIndexed { index, line -> "$index: $line" }.joinToString(System.lineSeparator()) }
  }
}
