package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.getTestName
import com.jetbrains.edu.csharp.toProjectModelEntity
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckUtils.fillWithIncorrect
import com.jetbrains.edu.learning.checker.CheckUtils.removeAttributes
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rd.util.reactive.adviseWithPrev
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.model.*
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.protocol.protocol
import com.jetbrains.rider.unitTesting.RiderUnitTestSessionConductor
import kotlinx.coroutines.*

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
    val name = task.getTestName()
    val rdUnitTestSession = getOrCreateSession(name) ?: return failedToCheck
    withContext(Dispatchers.EDT) {
      rdUnitTestSession.run.fire()
    }
    val isReady = CompletableDeferred<Unit>()
    withContext(Dispatchers.EDT) {
      rdUnitTestSession.state.adviseWithPrev(project.lifetime) { prev, cur ->
        if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
          isReady.complete(Unit)
        }
      }
      try {
        isReady.await()
      }
      catch (e: CancellationException) {
        project.solution.rdUnitTestHost.sessionManager.closeSession.fire(rdUnitTestSession.sessionId)
        return@withContext noTestsRun
      }
    }
    val checkResult = collectTestResults(rdUnitTestSession)
    return checkResult
  }

  private suspend fun getOrCreateSession(name: String): RdUnitTestSession? {
    val session = project.solution.rdUnitTestHost.sessions.values.firstOrNull { it.options.title.valueOrNull == name }
    if (session != null && isNotRunningState(session.state.valueOrNull?.status)) {
      return session
    }
    // if there is already a running session with the same tests, we need to close it and create a new one
    if (session != null) {
      withContext(Dispatchers.EDT) {
        project.solution.rdUnitTestHost.sessionManager.closeSession.fire(session.sessionId)
      }
    }
    return createUnitTestSession(name)
  }

  private suspend fun createUnitTestSession(name: String): RdUnitTestSession? = coroutineScope {
    val projectModelEntityId = task
                                 .toProjectModelEntity(project)
                                 ?.childrenEntities
                                 ?.firstOrNull { it.name == CSharpConfigurator.TEST_DIRECTORY }
                                 ?.getId(project)
                               ?: error("No project model entity associated with task ${task.name} found")
    try {
      val rdSession = CompletableDeferred<RdUnitTestSession>()
      withContext(Dispatchers.EDT) {
        val createdSessionId = project.solution.rdUnitTestHost.createSession.callSynchronously(
          RdProjectFolderCriterion(projectModelEntityId), project.protocol
        ) ?: return@withContext null
        project.solution.rdUnitTestHost.sessions.advise(project.lifetime) { entry ->
          val session = entry.newValueOpt ?: return@advise
          if (session.sessionId == createdSessionId) {
            session.options.title.set(name)
            rdSession.complete(session)
          }
        }
      }
      return@coroutineScope try {
        rdSession.await()
      }
      catch (e: CancellationException) {
        null
      }
    }
    catch (e: RdFault) {
      LOG.error(e.message)
      return@coroutineScope null
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
  ): Boolean {
    val result = CompletableDeferred<RdUnitTestResultData>()
    withContext(Dispatchers.EDT) {
      rdSession.resultData.advise(project.lifetime) { resultData ->
        if (resultData != null && resultData.nodeId == firstFailedNode.id) {
          result.complete(resultData)
        }
      }
      rdSession.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(firstFailedNode.id, true))
    }
    val resultData = try {
      result.await()
    }
    catch (e: CancellationException) {
      return false
    }
    val newInfo = (firstFailedNode.descriptor as RdUnitTestSessionNodeDescriptor).getEduTestInfo(resultData)
    testInfoWithNodes[firstFailedNode] = newInfo
    return true
  }

  private fun RdUnitTestSessionNodeDescriptor.getEduTestInfo(data: RdUnitTestResultData? = null): EduTestInfo {
    val (diff, infoLines) = tryGetDiff(data?.exceptionLines, text)
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

  private fun tryGetDiff(exceptionLines: String?, testName: String): Pair<CheckResultDiff?, String> {
    val lines = exceptionLines?.split("\n")?.takeWhile { !isPositionInfo(it, testName) } ?: return null to ""
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
  }
}
