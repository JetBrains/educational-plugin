package com.jetbrains.edu.ai.debugger.core.session

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.ai.debugger.core.breakpoint.AIBreakpointHintMouseMotionListener
import com.jetbrains.edu.ai.debugger.core.breakpoint.IntermediateBreakpointProcessor
import com.jetbrains.edu.ai.debugger.core.connector.AIDebuggerServiceConnector
import com.jetbrains.edu.ai.debugger.core.log.*
import com.jetbrains.edu.ai.debugger.core.messages.EduAIDebuggerCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.ml.debugger.dto.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) : Disposable {

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
        val fixes = withBackgroundProgress(
          project,
          EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session")
        ) {
          AIDebuggerServiceConnector.getInstance().getCodeFix(
            taskDescription = description,
            files = virtualFiles.toNumberedLineMap(),
            testDescription = testResult.details ?: testResult.message
          )
        }.onError { error ->
          unlock()
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session.fail")
          )
          AIDebuggerLogEntry(
            task = task.toTaskData(),
            actionType = "ErrorInRunDebugSession",
            testResult = testResult,
            testText = testText,
            userCode = virtualFiles.toStringPresentation(),
            error = "An error occurred. AI Debugging is currently unavailable: $error"
          ).logError()
          return@launch
        }

        val language = project.course?.languageById ?: error("Language is not found")
        val fileMap = virtualFiles.associateBy { it.name }
        val intermediateBreakpoints = calculateIntermediateBreakpointPositions(fixes, fileMap, language)
        fixes.toBreakpointPositionsByFileMap().toggleLineBreakpoint(fileMap, language)
        intermediateBreakpoints.toggleLineBreakpoint(fileMap, language)
        val breakpointHints = generateIntermediateBreakpointHints(fileMap, fixes, intermediateBreakpoints)
        if (breakpointHints == null) {
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session.fail")
          )
          return@launch
        }
        val listener = AIBreakpointHintMouseMotionListener(fixes, breakpointHints)
        EditorFactory.getInstance().eventMulticaster.apply {
          addEditorMouseMotionListener(listener, this@AIDebugSessionService)
          addEditorMouseListener(listener, this@AIDebugSessionService)
        }
        AIDebugSessionRunner(project, task, closeAIDebuggingHint, listener, language).runDebuggingSession(testResult)
        AIDebuggerLogEntry(
          task = task.toTaskData(),
          actionType = "RunDebugSession",
          testResult = testResult,
          testText = testText,
          userCode = virtualFiles.toStringPresentation(),
          fixes = fixes.content,
          intermediateBreakpoints = intermediateBreakpoints,
          breakpointHints = breakpointHints.content,
        ).logInfo()
      } catch (e: Exception) {
        unlock()
        LOG.error("An error occurred in the ai debugging session", e)
        AIDebuggerLogEntry(
          task = task.toTaskData(),
          actionType = "ErrorInRunDebugSession",
          testResult = testResult,
          testText = testText,
          userCode = virtualFiles.toStringPresentation(),
          error = "An error occurred in the ai debugging session: ${e.message}"
        ).logError()
      }
    }
  }

  private fun CodeFixResponse.toBreakpointPositionsByFileMap() =
    content.groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
      fileName to fixesForFile.map { it.wrongCodeLineNumber }
    }.toMap()

  private fun calculateIntermediateBreakpointPositions(
    fixes: CodeFixResponse,
    fileMap: Map<String, VirtualFile>,
    language: Language
  ) =
    fixes.content.groupBy { it.fileName }.mapNotNull { (fileName, fixesForFile) ->
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
    fixes: CodeFixResponse,
    intermediateBreakpointPositions: Map<String, List<Int>>
  ): BreakpointHintResponse? {
    val finalBreakpoints = fixes.content.mapNotNull {
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
      EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session")
    ) {
      AIDebuggerServiceConnector.getInstance().getBreakpointHint(fileMap.toNumberedLineMap(), finalBreakpoints, intermediateBreakpoint)
    }.onError { return null }
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

  override fun dispose() { }

}
