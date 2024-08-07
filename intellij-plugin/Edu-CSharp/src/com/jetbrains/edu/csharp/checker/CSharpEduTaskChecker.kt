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
    val result = CompletableDeferred<CheckResult>()
    subscribeToTestSessionResult(rdUnitTestSession, result)
    runInEdt {
      rdUnitTestSession.run.fire()
    }
    return runBlockingCancellable {
      while (!result.isCompleted) {
        if (indicator.isCanceled) {
          result.cancel()
          break
        }
      }
      if (result.isCancelled) {
        runInEdt {
          project.solution.rdUnitTestHost.sessionManager.closeSession.fire(rdUnitTestSession.sessionId)
        }
        return@runBlockingCancellable noTestsRun
      }
      return@runBlockingCancellable result.await()
    }
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

  private fun subscribeToTestSessionResult(session: RdUnitTestSession, result: CompletableDeferred<CheckResult>) = runInEdt {
    session.state.adviseWithPrev(project.lifetime) { prev, cur ->
      if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
        result.complete(collectTestResults(session))
      }
    }
  }

  private fun collectTestResults(rdSession: RdUnitTestSession): CheckResult {
    val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(rdSession.sessionId) ?: return noTestsRun
    val children = descriptor.getNodes().filter { !it.descriptor.hasChildren }
    val testInfo = children.map { (it.descriptor as RdUnitTestSessionNodeDescriptor).getEduTestInfo() }
    if (testInfo.isNotEmpty() && testInfo.firstFailed() == null) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = testInfo)
    }
    if (testInfo.isEmpty()) {
      return noTestsRun
    }
    return CheckResult(status = CheckStatus.Failed, executedTestsInfo = testInfo)
  }

  private fun RdUnitTestSessionNodeDescriptor.getEduTestInfo() = EduTestInfo(
    name = text,
    status = status.toPresentableStatus().value,
    message = removeAttributes(fillWithIncorrect(status.name)),
    details = statusMessage.trim(),
    isFinishedSuccessfully = status == RdUnitTestStatus.Success,
  )

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
