package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.jetbrains.edu.csharp.getTestName
import com.jetbrains.edu.csharp.toProjectModelEntity
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.CheckUtils.fillWithIncorrect
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
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
import kotlinx.coroutines.CompletableDeferred

class CSharpEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  EduTaskCheckerBase(task, envChecker, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val error = prepareEnvironment()
    if (error != null) {
      return error
    }
    val rdUnitTestSession = getOrCreateSession() ?: return noTestsRun
    val checkResult = CompletableDeferred<CheckResult>()
    runInEdt {
      rdUnitTestSession.run.fire()
    }
    subscribeToTestSessionResult(rdUnitTestSession, checkResult)
    while (!checkResult.isCompleted) {
      if (indicator.isCanceled) {
        checkResult.cancel()
        break
      }
    }
    if (checkResult.isCancelled) {
      runInEdt {
        project.solution.rdUnitTestHost.sessionManager.closeSession.fire(rdUnitTestSession.sessionId)
      }
      return noTestsRun
    }
    return runBlockingCancellable { checkResult.await() }
  }

  private fun getOrCreateSession(): RdUnitTestSession? {
    val name = task.getTestName()
    val session = project.solution.rdUnitTestHost.sessions.values.firstOrNull { it.options.title.valueOrNull == name }
    if (session != null && isNotRunningState(session.state.valueOrNull?.status)) {
      return session
    }
    else if (session != null) {
      project.solution.rdUnitTestHost.sessionManager.closeSession.fire(session.sessionId)
    }
    return runBlockingCancellable { createUnitTestSession(name) }
  }

  private suspend fun createUnitTestSession(name: String): RdUnitTestSession? {
    val projectModelEntityId = task.toProjectModelEntity(project)?.childrenEntities?.firstOrNull { it.name == "Test.cs" }?.getId(project)
                               ?: error("No project model entity associated with task ${task.name} found")
    try {
      // invokeAndWaitIfNeeded didn't work here, because of the read action:
      // logs said that such combination leads to deadlocks
      val rdSession = CompletableDeferred<RdUnitTestSession>()
      runInEdt {
        val createdSessionId = project.solution.rdUnitTestHost.createSession.callSynchronously(
          RdProjectFileCriterion(projectModelEntityId), project.protocol
        ) ?: error("Session creation failed")
        project.solution.rdUnitTestHost.sessions.advise(project.lifetime) { entry ->
          val session = entry.newValueOpt ?: return@advise
          if (session.sessionId == createdSessionId) {
            session.options.title.set(name)
            rdSession.complete(session)
          }
        }
      }
      return rdSession.await()
    }
    catch (e: RdFault) {
      LOG.error(e.message)
      return null
    }
  }

  private fun subscribeToTestSessionResult(session: RdUnitTestSession, checkResult: CompletableDeferred<CheckResult>) {
    val isReady = CompletableDeferred<Unit>()
    runInEdt {
      session.state.adviseWithPrev(project.lifetime) { prev, cur ->
        if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
          isReady.complete(Unit)
        }
      }
    }
    runBlockingCancellable { isReady.await() }
    checkResult.complete(collectTestResults(session))
  }

  private fun collectTestResults(rdSession: RdUnitTestSession): CheckResult {
    val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(rdSession.sessionId) ?: return noTestsRun
    val testTreeNodes = descriptor.getNodes().filter { !it.descriptor.hasChildren }
    val testInfoWithNodes = testTreeNodes.map { it to (it.descriptor as RdUnitTestSessionNodeDescriptor).getEduTestInfo() }.toMutableList()
    val firstFailedIndex =
      testInfoWithNodes.indexOfFirst { (it.first.descriptor as RdUnitTestSessionNodeDescriptor).status != RdUnitTestStatus.Success }
    if (firstFailedIndex != -1) {
      val firstFailedNode = testInfoWithNodes[firstFailedIndex].first
      val result = CompletableDeferred<RdUnitTestResultData>()
      runInEdt {
        rdSession.resultData.advise(project.lifetime) { resultData ->
          if (resultData != null && resultData.nodeId == firstFailedNode.id) {
            result.complete(resultData)
          }
        }
        rdSession.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(firstFailedNode.id, true))
      }
      val resultData = runBlockingCancellable { result.await() }
      val newInfo = testInfoWithNodes[firstFailedIndex].second.copy(
        details = resultData.exceptionLines,
        checkResultDiff = tryGetDiff(resultData.exceptionLines)
      )
      testInfoWithNodes[firstFailedIndex] = testInfoWithNodes[firstFailedIndex].copy(second = newInfo)
    }
    val testInfo = testInfoWithNodes.map { it.second }
    if (testInfo.isNotEmpty() && testInfo.firstFailed() == null) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = testInfo)
    }
    if (testInfoWithNodes.isEmpty()) {
      return noTestsRun
    }
    return CheckResult(status = CheckStatus.Failed, executedTestsInfo = testInfo)
  }

  private fun RdUnitTestSessionNodeDescriptor.getEduTestInfo(data: RdUnitTestResultData? = null) = EduTestInfo(
    name = text,
    status = status.toPresentableStatus().value,
    message = removeAttributes(fillWithIncorrect(status.name)),
    details = if (data != null && data.exceptionLines.isNotEmpty()) data.exceptionLines else statusMessage.trim(),
    isFinishedSuccessfully = status == RdUnitTestStatus.Success,
    checkResultDiff = tryGetDiff(data?.exceptionLines ?: "")
  )

  private fun tryGetDiff(lines: String): CheckResultDiff? {
    val split = lines.split("\n\n")[0].split("But was:") // hardcoded for now
    if (split.size < 2) return null
    val (expected, actual) = split
    return CheckResultDiff(expected.replace("Expected:", "").trim(), actual.trim())
  }

  private fun RdUnitTestStatus.toPresentableStatus() =
    when (this) {
      RdUnitTestStatus.Success -> EduTestInfo.PresentableStatus.COMPLETED
      RdUnitTestStatus.Failed -> EduTestInfo.PresentableStatus.FAILED
      RdUnitTestStatus.Running -> EduTestInfo.PresentableStatus.RUNNING
      RdUnitTestStatus.Aborted -> EduTestInfo.PresentableStatus.TERMINATED
      RdUnitTestStatus.Ignored -> EduTestInfo.PresentableStatus.IGNORED
      else -> EduTestInfo.PresentableStatus.ERROR
    }

  private fun isRunningState(data: RdUnitTestStatus?) = when (data) {
    RdUnitTestStatus.None -> false
    RdUnitTestStatus.Ignored -> false
    RdUnitTestStatus.Success -> false
    RdUnitTestStatus.Inconclusive -> false
    RdUnitTestStatus.Failed -> false
    RdUnitTestStatus.Aborted -> false
    RdUnitTestStatus.Pending -> true
    RdUnitTestStatus.Running -> true
    RdUnitTestStatus.Unknown -> false
    null -> false
  }

  private fun isNotRunningState(data: RdUnitTestStatus?) = !isRunningState(data)
}
