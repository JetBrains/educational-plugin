package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.DataManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val configuration = runReadAction { getConfiguration() } ?:
                        return CheckResult(CheckStatus.Unchecked, "No run configurations for this file.\n\n" +
                                                                  "Please run the source code and then click the `Run` button again.")

    StudyTaskManager.getInstance(project).course?.let {
      runInEdt {
        ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
      }
    }
    return CheckResult(CheckStatus.Solved, "")
  }

  private fun getTarget(): CMakeTarget? {
    val editor = EduUtils.getSelectedEditor(project) ?: return null
    val dataContext = DataManager.getInstance().getDataContext(editor.component)
    val context = ConfigurationContext.getFromContext(dataContext)
    val location = context.location ?: return null
    val targets = CidrTargetRunConfigurationProducer.getInstance(project)?.getExecutableTargetsForFile(location.psiElement.containingFile)
    if (targets == null || targets.isEmpty()) return null
    return targets.first() as CMakeTarget
  }

  private fun getConfiguration(): RunnerAndConfigurationSettings? {
    val runManager = RunManagerEx.getInstanceEx(project)
    val cmakeTarget = getTarget() ?: return null
    val configs = ContainerUtil.flatten(
        ContainerUtil.map<ConfigurationType, List<RunnerAndConfigurationSettings>>(
          ConfigurationType.CONFIGURATION_TYPE_EP.extensionList) {
          runManager.getConfigurationSettingsList(it)
        }
    )
    return configs.find { it.configuration.name == cmakeTarget.name }
  }
}
