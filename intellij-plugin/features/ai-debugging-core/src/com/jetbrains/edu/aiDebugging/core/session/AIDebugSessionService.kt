package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.aiDebugging.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.aiDebugging.core.breakpoint.IntermediateBreakpointProcessor
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.intellij.lang.Language
import com.intellij.openapi.application.readAction
import com.intellij.platform.ide.progress.withBackgroundProgress
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
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.BreakpointHintsResponse
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.atomic.AtomicBoolean


@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  private val lock = AtomicBoolean(false)

  fun unlock() {
    lock.set(false)
  }

  fun runDebuggingSession(
    task: Task,
    description: TaskDescription,
    virtualFiles: List<VirtualFile>,
    testResult: CheckResult,
    testText: String, // TODO: add a test to the request?
    closeAIDebuggingHint: () -> Unit
  ) {
    coroutineScope.launch {
      if (!lock.compareAndSet(false, true)) {
        LOG.error("AI Debug session is already running")
        return@launch
      }
      try {
        withBackgroundProgress(project, EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
          FixCodeForTestAssistant.getCodeFix(
            description,
            virtualFiles.toNumberedLineMap(),
            testResult.details ?: testResult.message
          )
        }.onSuccess { fixes ->
          val language = project.course?.languageById ?: error("Language is not found")
          val fileMap = virtualFiles.associateBy { it.name }
          val intermediateBreakpoints = calculateIntermediateBreakpointPositions(fixes, fileMap, language)
          fixes.toBreakpointPositionsByFileMap().toggleLineBreakpoint(fileMap, language)
          intermediateBreakpoints.toggleLineBreakpoint(fileMap, language)
          val breakpointHints = generateIntermediateBreakpointHints(fileMap, fixes, intermediateBreakpoints)
          if (breakpointHints == null) {
            EduNotificationManager.showErrorNotification(
              project,
              content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
            )
            return@onSuccess
          }
          AIDebugSessionRunner(project, task, closeAIDebuggingHint).apply {
            runDebuggingSession(testResult)
            subscribeToDebuggerEvents(fixes, breakpointHints)
          }
        }.onFailure {
          unlock()
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
          )
        }
      } catch (e: Exception) {
        unlock()
        LOG.error("An error occurred in the ai debugging session", e)
      }
    }
  }

  private fun FixCodeForTestResponse.toBreakpointPositionsByFileMap() =
    groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
      fileName to fixesForFile.map { it.wrongCodeLineNumber }
    }.toMap()

  private fun calculateIntermediateBreakpointPositions(
    fixes: FixCodeForTestResponse,
    fileMap: Map<String, VirtualFile>,
    language: Language
  ) =
    fixes.groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
      val virtualFile = fileMap[fileName] ?: return@mapNotNull null
      fileName to runReadAction {
        IntermediateBreakpointProcessor.calculateIntermediateBreakpointPositions(
          virtualFile,
          fixesForFile.map { it.wrongCodeLineNumber }.toList(),
          project,
          language
        )
      }
    }.toMap()

  private fun Map<String, List<Int>>.toggleLineBreakpoint(fileMap: Map<String, VirtualFile>, language: Language) {
    forEach { (fileName, positions) ->
      val virtualFile = fileMap[fileName] ?: error("Virtual file is not found")
      positions.forEach { position ->
        project.getService(AIBreakPointService::class.java)
          .toggleLineBreakpoint(language, virtualFile, position)
      }
    }
  }

  private suspend fun generateIntermediateBreakpointHints(
    fileMap: Map<String, VirtualFile>,
    fixes: FixCodeForTestResponse,
    intermediateBreakpointPositions: Map<String, List<Int>>
  ): BreakpointHintsResponse? {
    val finalBreakpoints = fixes.mapNotNull {
      val virtualFile = fileMap[it.fileName] ?: return@mapNotNull null
      val line = virtualFile.getLine(it.wrongCodeLineNumber)
      FinalBreakpoint(it.fileName, it.wrongCodeLineNumber, line, it.breakpointHint)
    }
    val intermediateBreakpoint = intermediateBreakpointPositions.map { (fileName, positions) ->
      positions.mapNotNull { position ->
        val virtualFile = fileMap[fileName] ?: return@mapNotNull null
        val line = virtualFile.getLine(position)
        IntermediateBreakpoint(fileName, position, line)
      }
    }.flatten()
    return withBackgroundProgress(
      project,
      EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")
    ) {
      BreakpointHintAssistant.getBreakpointHints(fileMap.toNumberedLineMap(), finalBreakpoints, intermediateBreakpoint)
    }.getOrNull()
  }

  private suspend fun VirtualFile.getLine(line: Int): String {
    val document = readAction { FileDocumentManager.getInstance().getDocument(this) } ?: error("Document is not found")
    if (line < 0 || line >= document.lineCount) {
      error("Line number $line is out of bounds")
    }
    return document.text.substring(document.getLineStartOffset(line), document.getLineEndOffset(line)).trim()
  }

  private fun List<VirtualFile>.toNumberedLineMap() = toNumberedLineMap { list ->
    list.asSequence().map { it.name to it }
  }

  private fun Map<String, VirtualFile>.toNumberedLineMap() = toNumberedLineMap { map ->
    map.asSequence().map { (name, virtualFile) -> name to virtualFile }
  }

  private fun <T> T.toNumberedLineMap(getVirtualFile: (T) -> Sequence<Pair<String, VirtualFile>>) = runReadAction {
    getVirtualFile(this).associate { (name, virtualFile) ->
      name to virtualFile.document.text.lines()
        .mapIndexed { index, line -> "$index: $line" }
        .joinToString(System.lineSeparator())
    }
  }

  companion object {
    private val LOG = Logger.getInstance(AIDebugSessionService::class.java)

    fun getInstance(project: Project): AIDebugSessionService = project.getService(AIDebugSessionService::class.java)
  }

}
