package com.jetbrains.edu.csharp.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.csharp.getCSProjFileNameWithoutExtension
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.rd.util.reactive.RdFault
import com.jetbrains.rdclient.util.idea.callSynchronously
import com.jetbrains.rider.model.RdProjectFileCriterion
import com.jetbrains.rider.model.RdUnitTestResultData
import com.jetbrains.rider.model.RdUnitTestStatus
import com.jetbrains.rider.model.rdUnitTestHost
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.findProjectsByName
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.protocol.protocol
import com.jetbrains.rider.run.configurations.unitTesting.RiderUnitTestRunConfigurationType

class CSharpEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  EduTaskCheckerBase(task, envChecker, project) {

  private val testResults = mutableListOf<RdUnitTestResultData>()
  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    testResults.clear()
    val runManager = RunManager.getInstance(project)
    val name = task.getCSProjFileNameWithoutExtension().filter { it != '.' } + "Test"
    val confs = runManager.findConfigurationByTypeAndName(RiderUnitTestRunConfigurationType.runConfigId, name)
    if (confs == null) {
      val id = WorkspaceModel.getInstance(project).findProjectsByName(task.getCSProjFileNameWithoutExtension())
                 .firstOrNull()?.childrenEntities?.firstOrNull { it.name == "Test.cs" }?.getId(project) ?: return emptyList()

      runInEdt { // needs to be run in EDT (and synchronously, and, apparently, now it is not the case)
        try {
          // create a new test session to run (`createSession` is the new api)
          val createdSessionId = project.solution.rdUnitTestHost.createSession.callSynchronously(
            RdProjectFileCriterion(id),
            project.protocol
          ) ?: return@runInEdt
          // val descriptor = RiderUnitTestSessionConductor.getInstance(project).getSessionDescriptorById(sessionId) ?: return@advise
          // val rdSession = descriptor.rdSession
          project.solution.rdUnitTestHost.sessions.view(project.lifetime) { _, sessionId, rdSession ->
            if (sessionId == createdSessionId) {
              // set proper name (get session via `RiderUnitTestSessionConductor` does not work, since the new session has not yet appeared there)
              rdSession.options.title.set(name)
              // collect test results by hand (obviously, a temporary solution)
              rdSession.resultData.advise(project.lifetime) { resultData ->
                if (resultData != null && resultData.status != RdUnitTestStatus.Pending) {
                  testResults.add(resultData)
                }
              }
            }
          }
        }
        catch (e: RdFault) {
          println("RDFAULT: ${e.message}")
        }
      }
    }
    // new session created -> it appears as a run configuration
    return listOfNotNull(runManager.findConfigurationByTypeAndName(RiderUnitTestRunConfigurationType.runConfigId, name))
  }

  override fun fillResults(): List<CheckResult> = testResults.map { it.toCheckResult() }

  private fun RdUnitTestResultData.toCheckResult(): CheckResult {
    if (this.status == RdUnitTestStatus.Success) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS)
    }
    return CheckResult(status = CheckStatus.Failed)
  }
}