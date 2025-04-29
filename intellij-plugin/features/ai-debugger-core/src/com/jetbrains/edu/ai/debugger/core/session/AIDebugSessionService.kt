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
import com.jetbrains.edu.ai.debugger.core.service.TaskDescription
import com.jetbrains.edu.ai.debugger.core.service.TestInfo
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getLanguage
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.getTaskDescriptionText
import com.jetbrains.edu.ai.debugger.core.utils.AIDebugUtils.toTaskDescriptionType
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
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
    userSolution: Map<String, String>,
    virtualFileMap: Map<String, VirtualFile>,
    testResult: CheckResult,
    testInfo: TestInfo,
    closeAIDebuggingHint: () -> Unit
  ) {
    coroutineScope.launch {
      if (!lock.compareAndSet(false, true)) {
        LOG.error("AI Debug session is already running")
        return@launch
      }
      try {
        val finalBreakpoints = withBackgroundProgress(
          project,
          EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session")
        ) {
          AIDebuggerServiceConnector.getInstance().getBreakpoints(
            authorSolution = task.taskFiles.values.filter { !it.isTestFile }.associate { it.name to it.getSolution() },
            // TODO: positive number / non-nullable
            courseId = project.course?.id ?: 0,
            programmingLanguage = project.getLanguage(),
            taskDescription = TaskDescription(
              descriptionFormat = task.descriptionFormat.toTaskDescriptionType(),
              text = task.getTaskDescriptionText(project)
            ),
            taskId = task.id,
            testInfo = testInfo,
            // TODO: positive number
            updateVersion = project.course?.marketplaceCourseVersion,
            userSolution = userSolution,
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
            testText = testInfo.text,
            userCode = userSolution.toString(),
            error = "An error occurred. AI Debugging is currently unavailable: $error"
          ).logError()
          return@launch
        }

        val language = project.course?.languageById ?: error("Language is not found")
        val intermediateBreakpoints = calculateIntermediateBreakpointPositions(finalBreakpoints, virtualFileMap, language)
        finalBreakpoints.toBreakpointPositionsByFileMap().toggleLineBreakpoint(virtualFileMap, language)
        intermediateBreakpoints.toggleLineBreakpoint(virtualFileMap, language)
        val breakpointHints = generateBreakpointHints(virtualFileMap, finalBreakpoints, intermediateBreakpoints)
        if (breakpointHints == null) {
          EduNotificationManager.showErrorNotification(
            project,
            content = EduAIDebuggerCoreBundle.message("action.Educational.AiDebuggerNotification.modal.session.fail")
          )
          unlock()
          return@launch
        }
        val listener = AIBreakpointHintMouseMotionListener(breakpointHints)
        EditorFactory.getInstance().eventMulticaster.apply {
          addEditorMouseMotionListener(listener, this@AIDebugSessionService)
          addEditorMouseListener(listener, this@AIDebugSessionService)
        }
        AIDebugSessionRunner(project, task, closeAIDebuggingHint, listener, language).runDebuggingSession(testResult)
        AIDebuggerLogEntry(
          task = task.toTaskData(),
          actionType = "RunDebugSession",
          testResult = testResult,
          testText = testInfo.text,
          userCode = userSolution.toString(),
          finalBreakpoints = finalBreakpoints,
          intermediateBreakpoints = intermediateBreakpoints,
          breakpointHints = breakpointHints,
        ).logInfo()
      } catch (e: Exception) {
        unlock()
        LOG.error("An error occurred in the ai debugging session", e)
        AIDebuggerLogEntry(
          task = task.toTaskData(),
          actionType = "ErrorInRunDebugSession",
          testResult = testResult,
          testText = testInfo.text,
          userCode = userSolution.toString(),
          error = "An error occurred in the ai debugging session: ${e.message}"
        ).logError()
      }
    }
  }

  private fun List<Breakpoint>.toBreakpointPositionsByFileMap() =
    groupBy { it.fileName }.mapNotNull { (fileName, breakpoints) ->
      fileName to breakpoints.map { it.lineNumber }
    }.toMap()

  private fun calculateIntermediateBreakpointPositions(
    finalBreakpoints: List<Breakpoint>,
    fileMap: Map<String, VirtualFile>,
    language: Language
  ) =
    finalBreakpoints.groupBy { it.fileName }.mapNotNull { (fileName, breakpoints) ->
      val virtualFile = fileMap[fileName] ?: return@mapNotNull null
      fileName to runReadAction {
        IntermediateBreakpointProcessor.calculateIntermediateBreakpointPositions(
          virtualFile,
          breakpoints.map { it.lineNumber },
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

  private suspend fun generateBreakpointHints(
    fileMap: Map<String, VirtualFile>,
    finalBreakpoints: List<Breakpoint>,
    intermediateBreakpointPositions: Map<String, List<Int>>
  ): List<BreakpointHintDetails>? {
    val intermediateBreakpoint = intermediateBreakpointPositions.map { (fileName, positions) ->
      positions.mapNotNull { position ->
        val virtualFile = fileMap[fileName] ?: return@mapNotNull null
        val line = virtualFile.getLine(position)
        Breakpoint(fileName, position, line)
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
