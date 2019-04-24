package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.ide.DataManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.cidr.execution.ExecutableData
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val configuration = getConfiguration() ?: return CheckResult(CheckStatus.Unchecked, CheckUtils.NOT_RUNNABLE_MESSAGE)

    StudyTaskManager.getInstance(project).course?.let {
      runInEdt {
        ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
      }
    }
    return CheckResult(CheckStatus.Solved, "")
  }

  private fun getConfiguration(): RunnerAndConfigurationSettings? {
    val runManager = RunManagerEx.getInstanceEx(project) as RunManagerImpl
    val helper = CMakeRunConfigurationType.getHelper(project)
    val cmakeTarget = getTarget() ?: return null
    val buildConfiguration = helper.getDefaultConfiguration(cmakeTarget)
    val buildTarget = BuildTargetData(cmakeTarget)

    return createRunConfiguration(runManager,
                                  helper.getDefaultTargetType(cmakeTarget).factory,
                                  cmakeTarget.name,
                                  BuildTargetAndConfigurationData(buildTarget,
                                                                  if (buildConfiguration == null) null else buildConfiguration.getName()),
                                  if (cmakeTarget.isExecutable) Ref.create(ExecutableData(buildTarget)) else null)
  }

  private fun getTarget(): CMakeTarget? {
    val editor = EduUtils.getSelectedEditor(project) ?: return null
    val dataContext = DataManager.getInstance().getDataContext(editor.component)
    val context = ConfigurationContext.getFromContext(dataContext)
    val location = context.location ?: return null
    val targetRunConfigurationInstance = CidrTargetRunConfigurationProducer.getInstance(project) ?: return null
    val targets = targetRunConfigurationInstance.getExecutableTargetsForFile(location.psiElement.containingFile)
    if (targets.isEmpty()) return null

    return targets.first() as CMakeTarget
  }

  private fun createRunConfiguration(runManager: RunManagerEx,
                                     factory: ConfigurationFactory,
                                     name: String,
                                     data: BuildTargetAndConfigurationData,
                                     executableData: Ref<ExecutableData>?): RunnerAndConfigurationSettings {
    val result = runManager.createConfiguration(name, factory)
    val runConfig = result.configuration as CMakeAppRunConfiguration
    runConfig.targetAndConfigurationData = data
    if (executableData != null) runConfig.executableData = executableData.get()
    return result
  }
}
