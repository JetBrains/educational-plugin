package com.jetbrains.edu.csharp.checker

import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.util.ui.tree.TreeModelAdapter
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
import com.jetbrains.rider.unitTesting.RiderUnitTestConsoleHyperlinkFilter
import com.jetbrains.rider.unitTesting.RiderUnitTestSessionConductor
import kotlinx.coroutines.CompletableDeferred
import javax.swing.event.TreeModelEvent

class CSharpEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  EduTaskCheckerBase(task, envChecker, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val error = prepareEnvironment()
    if (error != null) {
      return error
    }
    val results = CompletableDeferred<List<RdUnitTestTreeNode>>()
    val rdUnitTestSession = getOrCreateSession() ?: return noTestsRun
    // TODO: check build successful
//    val result = CompletableDeferred<CheckResult>()
    subscribeToResults(rdUnitTestSession, results)
    runInEdt {
      rdUnitTestSession.run.fire()
    }
    val testResults = runBlockingCancellable {
      while (!results.isCompleted) {
        if (indicator.isCanceled) {
          results.cancel()
          break
        }
      }
      if (results.isCancelled) {
        runInEdt {
          project.solution.rdUnitTestHost.sessionManager.closeSession.fire(rdUnitTestSession.sessionId)
        }
        return@runBlockingCancellable emptyList()
      }
      results.await()
    }

    val res = ConcurrentCollectionFactory.createConcurrentSet<EduTestInfo>()//ConcurrentSet<EduTestInfo>()
    val result = CompletableDeferred<CheckResult>()
    runInEdt {
      rdUnitTestSession.resultData.advise(project.lifetime) { resultData ->
        println("DATA: ${dataToString(resultData)}")
        if (resultData != null && resultData.sessionId == rdUnitTestSession.sessionId && isNotRunningState(resultData.status)) {
          res.add(getEduTestInfo(resultData))
          if (res.size == testResults.size) {
            result.complete(collectTestResults(res))
          }
        }
      }
    }
    testResults.forEach {
      runInEdt {
        rdUnitTestSession.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(it.id, false))

//        rdUnitTestSession.treeDescriptor
//        val descriptor = RiderUnitTestSessionConductor.getInstance(project).getConfigurationSettings(rdUnitTestSession.sessionId)
//        rdUnitTestSession.resultData
      }
    }
    return runBlockingCancellable { result.await() }
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


  private fun subscribeToResults(session: RdUnitTestSession, childrenTests: CompletableDeferred<List<RdUnitTestTreeNode>>) = runInEdt {
//    val childrenTests = CompletableDeferred<List<RdUnitTestTreeNode>>()
    session.state.adviseWithPrev(project.lifetime) { prev, cur ->
      if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
        val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(session.sessionId)
                         ?: run { childrenTests.complete(emptyList()); return@adviseWithPrev }
        val children = descriptor.getNodes().filter { !it.descriptor.hasChildren }
        childrenTests.complete(children)
      }
    }



//    println("TITLE: ${descriptor.title.valueOrNull}")
//    session.resultData.advise(project.lifetime) { resultData ->
//      println("RDSESSION: ${session.sessionId} ${resultData?.sessionId} ${dataToString(resultData)}")
//      val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(session.sessionId)
//                       ?: run { result.complete(noTestsRun); return@advise }
//      val children = descriptor.getNodes().filter { !it.descriptor.hasChildren }
//      children.forEach { runInEdt { session.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(it.id, true)) } }
//
////      val treeDescriptor = RiderUnitTestTreeSessionDescriptor(project.lifetime, project, session.treeDescriptor, session)
//
//      if (resultData != null && session.sessionId == resultData.sessionId) {
//        if (children.all { it.descriptor.text != resultData.text }) {
//          return@advise
//        }
//        result.complete(collectTestResults(resultData, session))
//      }
//    }
  }


//  private fun subscribeToTestSessionResult(session: RdUnitTestSession, result: CompletableDeferred<RdUnitTestResultData>) = runInEdt {
//    session.state.adviseWithPrev(project.lifetime) { prev, cur ->
//      if (isRunningState(prev.asNullable?.status) && isNotRunningState(cur.status)) {
//        project.solution.rdUnitTestHost.sessions.view(project.lifetime) { _, sessionId, rdSession ->
//          val resultData = rdSession.resultData.value
//          if (sessionId == session.sessionId && resultData != null) {
//            result.complete()
//          }
//        }
//      }
//    }
//  }


  private fun collectTestResults(testsInfoSet: Set<EduTestInfo>): CheckResult {
//    val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(rdSession.sessionId) ?: return noTestsRun
//    val children = descriptor.getNodes().filter { !it.descriptor.hasChildren }
//    children.forEach { runInEdt { rdSession.treeDescriptor.selectNode.fire(RdUnitTestNavigateArgs(it.id, true)) } }
//    println("DIAGNOSTICS: ${children.map { child -> RiderUnitTestDiagnostics.display(child) + "\n" }}")
//    val testsInfo = getEduTestInfo(result)
    val testsInfo = testsInfoSet.toList()
    if (testsInfo.firstFailed() != null) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = testsInfo)
    }
    if (testsInfo.isNotEmpty() && testsInfo.firstFailed() == null) {
      error("Testing failed although no failed tests found")
    }
    return CheckResult(status = CheckStatus.Failed, executedTestsInfo = testsInfo)
  }

  private fun dataToString(data: RdUnitTestResultData?): String {
    if (data == null)
      return ""

    val trimMessage = when (data.isTrimmed) {
      true -> "The output has reached the limit and was truncated.\n" +
              "To view the full output use the ${RiderUnitTestConsoleHyperlinkFilter.steLinkText} action.\n" +
              "You can also change the limit in ${RiderUnitTestConsoleHyperlinkFilter.settingsLinkText}."

      false -> ""
    }

    val diagnosticsMessage = listOf(
      "-------------------------------------------",
      "Text              = " + data.text,
      "TestCount         = " + data.testCount,
      "Status            = " + data.status.toString(),
      "StatusMessage     = " + data.statusMessage,
      "SessionId         = " + data.sessionId,
      "ElementId         = " + data.elementId,
      "Duration          = " + data.duration,
      "FailedTestCount   = " + data.failedTestCount,
      "IsOutdated        = " + data.isOutdated
    ).joinToString("\n")

    val statusMessage = when {
      data.statusMessage.isEmpty() -> ""
      data.exceptionLines.contains(data.statusMessage) -> ""
      data.outputLines.contains(data.statusMessage) -> ""
      else -> data.statusMessage.trim()
    }

    val listOfMessages = listOf(
      //data.elementId,
      statusMessage,
      data.exceptionLines,
      data.outputLines,
      data.serviceMessage,
      trimMessage,
      diagnosticsMessage
    )

    return listOfMessages
      .filter { it.isNotEmpty() }
      .joinToString("\n\n")
      .trim()
  }

  private fun RdUnitTestSessionNodeDescriptor.getEduTestInfo(data: RdUnitTestResultData?) = EduTestInfo(
    name = text,
    status = status.toPresentableStatus().value,
    message = removeAttributes(fillWithIncorrect(status.name)),
    details = data?.exceptionLines, //statusMessage.trim(),
    isFinishedSuccessfully = status == RdUnitTestStatus.Success,
  )

  private fun getEduTestInfo(data: RdUnitTestResultData) = EduTestInfo(
    name = data.text,
    status = data.status.toPresentableStatus().value,
    message = removeAttributes(fillWithIncorrect(data.status.name)),
    details = data.exceptionLines, //statusMessage.trim(),
    isFinishedSuccessfully = data.status == RdUnitTestStatus.Success,
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
