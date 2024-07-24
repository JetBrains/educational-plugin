package com.jetbrains.edu.go.checker

import com.goide.psi.GoFile
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.edu.go.checker.GoEduTaskChecker.Companion.GO_RUN_WITH_PTY
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.DefaultCodeExecutor
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.toPsiFile
import com.jetbrains.edu.learning.withRegistryKeyOff

class GoCodeExecutor : DefaultCodeExecutor() {
  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val psiFile = getMainFile(project, task) ?: return null
    return ConfigurationContext(psiFile).configuration
  }

  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    return withRegistryKeyOff(GO_RUN_WITH_PTY) { super.execute(project, task, indicator, input) }
  }

  private fun getMainFile(project: Project, task: Task): PsiFile? {
    for ((_, file) in task.taskFiles) {
      val psiFile = file.getDocument(project)?.toPsiFile(project) ?: continue
      if (psiFile is GoFile && psiFile.hasMainFunction()) {
        return psiFile
      }
    }
    return null
  }
}