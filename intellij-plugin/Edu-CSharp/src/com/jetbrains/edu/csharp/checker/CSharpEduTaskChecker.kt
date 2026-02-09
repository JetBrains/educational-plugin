package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.getTestName
import com.jetbrains.edu.csharp.toProjectModelEntity
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.fillWithIncorrect
import com.jetbrains.edu.learning.checker.CheckUtils.removeAttributes
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.adviseWithPrev
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.build.BuildEventsService
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.build.tasks.BuildTaskThrottler
import com.jetbrains.rider.model.*
import com.jetbrains.rider.model.build.BuildEvent
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import com.jetbrains.rider.protocol.protocol
import com.jetbrains.rider.unitTesting.RiderUnitTestSessionConductor
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class CSharpEduTaskChecker(task: EduTask, private val envChecker: EnvironmentChecker, project: Project) :
  TaskChecker<EduTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val error = prepareEnvironment()
    if (error != null) {
      return error
    }
    return runBlockingCancellable { asyncCheck() }
  }

  private fun prepareEnvironment(): CheckResult? {
    if (task.course.isStudy) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
      }
    }

    val possibleError = envChecker.getEnvironmentError(project, task)
    return possibleError
  }

  private suspend fun asyncCheck(): CheckResult {
    // For now we manually build each test before execution,
    // should be dropped once the fix is implemented on the Rider's side. See EDU-8599
    val compilationError = tryBuildTaskProject()
    if (compilationError != null) {
      return compilationError
    }

    val name = task.getTestName()
    val rdUnitTestSession = getOrCreateSession(name) ?: return failedToCheck
    val isReady = CompletableDeferred<Unit>()

    withContext(Dispatchers.EDT) {
      // Since we already manually build each test before execution,
      // we use this policy to avoid double builds.
      // See EDU-8599
      rdUnitTestSession.options.buildPolicy.set(RdUnitTestBuildPolicy.Never)
      rdUnitTestSession.run.fire()
    }

    return project.lifetime.usingNested { lt ->
      withContext(Dispatchers.EDT) {
        rdUnitTestSession.state.adviseWithPrev(lt) { prev, cur ->
          if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
            isReady.complete(Unit)
          }
        }
      }

      try {
        isReady.await()
      }
      catch (_: CancellationException) {
        withContext(Dispatchers.EDT) {
          project.solution.rdUnitTestHost.sessionManager.closeSession.fire(rdUnitTestSession.sessionId)
        }
        return@usingNested noTestsRun
      }

      collectTestResults(rdUnitTestSession)
    }
  }

  private suspend fun tryBuildTaskProject(): CheckResult? = project.lifetime.usingNested { buildLifetime ->
    doTryBuildTaskProject(buildLifetime)
  }

  private suspend fun doTryBuildTaskProject(buildLifetime: Lifetime): CheckResult? {
    val isBuildFailed = CompletableDeferred<String>()
    val buildEventsService = project.service<BuildEventsService>()

    withContext(Dispatchers.EDT) {
      buildEventsService.getEventsAndSubscribe(buildLifetime) { events ->
        val errorMessages = events.joinToString("\n") { eventRef ->
          val buildEvent = buildEventsService.getBuildEvent(eventRef.offset)
          val sourceFile = buildEvent.filePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
          val taskProjectId = task.toProjectModelEntity(project)?.getId(project)

          // `getEventsAndSubscribe` receives all the events, not necessarily related to our current task
          if (eventRef.projectId == taskProjectId) {
            formatBuildEvent(buildEvent, sourceFile, eventRef).joinToString("\n")
          }
          else {
            ""
          }
        }

        if (errorMessages.isNotEmpty()) {
          isBuildFailed.complete(errorMessages)
        }
      }
    }

    val projectFilePath = task.toProjectModelEntity(project)?.url?.virtualFile?.path ?: return failedToCheck

    val buildParameters = BuildParameters(
      operation = BuildTarget(),
      selectedProjectsPaths = listOf(projectFilePath),
      silentMode = true,
      diagnosticsMode = false,
      withoutDependencies = false,
      noRestore = false
    )
    val buildResult = project.service<BuildTaskThrottler>().buildSequentially(buildParameters)

    if (buildResult.buildResultKind == BuildResultKind.HasErrors) {
      val err = try {
        withTimeout(500.milliseconds) {
          isBuildFailed.await()
        }
      }
      catch (_: TimeoutCancellationException) {
        LOG.warn("Timeout waiting for build events for task ${task.name}")
        ""
      }
      return CheckResult(CheckStatus.Failed, COMPILATION_FAILED_MESSAGE, err.trimEnd('\n'))
    }
    return null
  }

  /**
   * Format build output, implementation borrowed from Rider
   */
  private fun formatBuildEvent(buildEvent: BuildEvent, sourceFile: VirtualFile?, eventRef: BuildEventRef): Array<String> {
    val message = if (eventRef.offset == -1L && eventRef.customMessage != null) eventRef.customMessage else buildEvent.message
    return if (sourceFile == null) {
      message ?: ""
    }
    else {
      val code = if (buildEvent.code != null) " [${buildEvent.code}]" else ""
      "${sourceFile.name}${formatLine(buildEvent.line ?: 0, buildEvent.column ?: 0)}:$code $message"
    }.split('\r', '\n').filter { it.isNotEmpty() }.toTypedArray()
  }

  private fun formatLine(line: Int, column: Int) = when {
    line > 0 && column > 0 -> "($line, $column)"
    line > 0 -> "($line)"
    else -> ""
  }

  private suspend fun getOrCreateSession(name: String): RdUnitTestSession? {
    val session = project.solution.rdUnitTestHost.sessions.values.firstOrNull { it.options.title.valueOrNull == name }
    if (session != null && isNotRunningState(session.state.valueOrNull?.status) && session.state.valueOrNull?.status != RdUnitTestStatus.None) {
      return session
    }
    // if there is already a session with the same tests, we need to close it and create a new one
    session?.close()
    return createUnitTestSession(name)
  }

  private suspend fun RdUnitTestSession.close() = project.lifetime.usingNested { lt ->
    val sessionClosed = CompletableDeferred<Unit>()
    withContext(Dispatchers.EDT) {
      project.solution.rdUnitTestHost.sessions.advise(lt) { entry ->
        if (entry.key == sessionId && entry.newValueOpt == null) {
          // wait for the session to be closed
          sessionClosed.complete(Unit)
        }
      }
      project.solution.rdUnitTestHost.sessionManager.closeSession.fire(sessionId)
    }
    try {
      withTimeout(CLOSE_SESSION_TIMEOUT) {
        sessionClosed.await()
      }
    }
    catch (_: TimeoutCancellationException) {
      LOG.warn("Timeout waiting for session to close: $sessionId")
    }
  }

  suspend fun getRdUnitTestCriterion(): RdUnitTestCriterion? = withContext(Dispatchers.EDT) {
    val testsUrl = task.getDir(project.courseDir)?.findChild(CSharpConfigurator.TEST_DIRECTORY)?.url ?: return@withContext null

    val virtualFileUrl = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
      .findByUrl(testsUrl)
    if (virtualFileUrl == null) {
      LOG.error("No test directory found for task ${task.name}: $testsUrl")
      return@withContext null
    }
    val projectModelId = WorkspaceModel.getInstance(project).getProjectModelEntities(virtualFileUrl).firstOrNull()?.getId(project)

    if (projectModelId == null) {
      LOG.error("No project model entity associated with task ${task.name} found: $testsUrl")
      return@withContext null
    }

    return@withContext RdProjectFolderCriterion(projectModelId)
  }

  private suspend fun createUnitTestSession(name: String): RdUnitTestSession? = project.lifetime.usingNested { lt ->
    val unitTestCriterion = getRdUnitTestCriterion() ?: return@usingNested null
    try {
      val rdSession = CompletableDeferred<RdUnitTestSession?>()

      withContext(Dispatchers.EDT) {
        val createdSessionId = project.solution.rdUnitTestHost.createSession.callSynchronously(unitTestCriterion, project.protocol)
        if (createdSessionId == null) {
          rdSession.complete(null)
          return@withContext
        }

        project.solution.rdUnitTestHost.sessions.advise(lt) { entry ->
          val session = entry.newValueOpt ?: return@advise
          if (session.sessionId == createdSessionId) {
            session.options.title.set(name)
            rdSession.complete(session)
          }
        }
      }
      rdSession.await()
    }
    catch (e: RdFault) {
      LOG.error(e)
      null
    }
  }

  private suspend fun collectTestResults(rdSession: RdUnitTestSession): CheckResult {
    val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(rdSession.sessionId) ?: return noTestsRun
    val testInfoWithNodes = descriptor.getNodes()
      .filter { !it.descriptor.hasChildren }
      .associateWithNotNullTo(HashMap()) { (it.descriptor as? RdUnitTestSessionNodeDescriptor)?.getEduTestInfo() }

    val firstFailedNode = testInfoWithNodes.keys.firstOrNull {
      (it.descriptor as RdUnitTestSessionNodeDescriptor).status != RdUnitTestStatus.Success
    }
    if (firstFailedNode != null) {
      if (!getFirstFailedTestInfo(rdSession, testInfoWithNodes, firstFailedNode)) {
        return failedToCheck
      }
    }
    val testInfo = testInfoWithNodes.map { it.value }
    if (testInfo.isNotEmpty() && testInfo.firstFailed() == null) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = testInfo)
    }
    if (testInfoWithNodes.isEmpty()) {
      return noTestsRun
    }
    return CheckResult(status = CheckStatus.Failed, executedTestsInfo = testInfo)
  }

  private suspend fun getFirstFailedTestInfo(
    rdSession: RdUnitTestSession,
    testInfoWithNodes: HashMap<RdUnitTestTreeNode, EduTestInfo>,
    firstFailedNode: RdUnitTestTreeNode
  ): Boolean = project.lifetime.usingNested { lt ->
    val result = CompletableDeferred<RdUnitTestResultData>()
    withContext(Dispatchers.EDT) {
      rdSession.sessionOutput.advise(lt) { sessionOutput ->
        sessionOutput.resultData.advise(lt) { resultData ->
          if (resultData != null && resultData.nodeId == firstFailedNode.id) {
            result.complete(resultData)
          }
        }
      }
      rdSession.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(firstFailedNode.id, true))
    }
    val resultData = withTimeout(500.milliseconds) {
      result.await()
    }
    val newInfo = (firstFailedNode.descriptor as RdUnitTestSessionNodeDescriptor).getEduTestInfo(resultData)
    testInfoWithNodes[firstFailedNode] = newInfo
    true
  }

  private fun RdUnitTestSessionNodeDescriptor.getEduTestInfo(data: RdUnitTestResultData? = null): EduTestInfo {
    val (diff, infoLines) = if (data != null) {
      tryGetDiff(data.exceptionLines, text)
    }
    else {
      null to ""
    }
    val message = infoLines.ifBlank { status.name }
    return EduTestInfo(
      name = text,
      status = status.toPresentableStatus().value,
      message = removeAttributes(fillWithIncorrect(message)),
      details = if (data != null && data.exceptionLines.isNotEmpty()) data.exceptionLines else statusMessage.trim(),
      isFinishedSuccessfully = status == RdUnitTestStatus.Success || status == RdUnitTestStatus.Ignored,
      checkResultDiff = diff
    )
  }

  private fun tryGetDiff(exceptionLines: String, testName: String): Pair<CheckResultDiff?, String> {
    val lines = exceptionLines.split("\n").takeWhile { !isPositionInfo(it, testName) }
    val message = lines.takeWhile { !it.trim().startsWith(EXPECTED) }.joinToString("\n")
    val expected = lines.firstOrNull { it.trim().startsWith(EXPECTED) }?.replace(EXPECTED, "")
    val actual = lines.firstOrNull { it.trim().startsWith(ACTUAL) }?.replace(ACTUAL, "")
    if (expected == null || actual == null) {
      return null to message
    }
    return CheckResultDiff(expected.trim(), actual.trim()) to message
  }

  private fun isPositionInfo(line: String, testName: String): Boolean {
    return line.trim().startsWith("at ") && line.trim().contains("$testName()")
  }

  private fun RdUnitTestStatus.toPresentableStatus(): EduTestInfo.PresentableStatus =
    when (this) {
      RdUnitTestStatus.Success -> EduTestInfo.PresentableStatus.COMPLETED
      RdUnitTestStatus.Failed -> EduTestInfo.PresentableStatus.FAILED
      RdUnitTestStatus.Running -> EduTestInfo.PresentableStatus.RUNNING
      RdUnitTestStatus.Aborted -> EduTestInfo.PresentableStatus.TERMINATED
      RdUnitTestStatus.Ignored -> EduTestInfo.PresentableStatus.IGNORED
      else -> EduTestInfo.PresentableStatus.ERROR
    }

  private fun <K, V, M : MutableMap<in K, in V>> Iterable<K>.associateWithNotNullTo(destination: M, valueSelector: (K) -> V?): M {
    for (element in this) {
      valueSelector(element)?.let {
        destination.put(element, it)
      }
    }
    return destination
  }

  private fun isRunningState(data: RdUnitTestStatus?): Boolean = data == RdUnitTestStatus.Pending || data == RdUnitTestStatus.Running

  private fun isNotRunningState(data: RdUnitTestStatus?): Boolean = !isRunningState(data)

  companion object {
    const val EXPECTED = "Expected: "
    const val ACTUAL = "But was: "
    private val CLOSE_SESSION_TIMEOUT = 5000.milliseconds

    private val LOG = logger<CSharpEduTaskChecker>()
  }
}