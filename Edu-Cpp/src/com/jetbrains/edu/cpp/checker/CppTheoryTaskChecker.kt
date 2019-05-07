package com.jetbrains.edu.cpp.checker

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.DataManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer
import com.jetbrains.cidr.execution.CidrTargetRunLineMarkerProvider
import com.jetbrains.cidr.lang.psi.OCFunctionDeclaration
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class CppTheoryTaskChecker(task: TheoryTask, project: Project) : TheoryTaskChecker(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val configuration = runReadAction { getConfiguration() } ?:
                        return CheckResult(CheckStatus.Unchecked, "This file has no `main()` method to run.")
    runInEdt {
      ProgramRunnerUtil.executeConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    }
    return CheckResult(CheckStatus.Solved, "")
  }

  private fun getConfiguration(): RunnerAndConfigurationSettings? {
    val editor = EduUtils.getSelectedEditor(project) ?: return null
    val dataContext = DataManager.getInstance().getDataContext(editor.component)
    val context = ConfigurationContext.getFromContext(dataContext)
    val location = context.location ?: return null
    val functions = PsiTreeUtil.findChildrenOfType(location.psiElement.containingFile, OCFunctionDeclaration::class.java)
    val mainFunction = functions.find { CidrTargetRunLineMarkerProvider.isInEntryPointBody(it) } ?: return null
    val fromContext = CidrTargetRunConfigurationProducer.getInstance(project)
      ?.findOrCreateConfigurationFromContext(ConfigurationContext(mainFunction))
    return fromContext?.configurationSettings
  }
}
