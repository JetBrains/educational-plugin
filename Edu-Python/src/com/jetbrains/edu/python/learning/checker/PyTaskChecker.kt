package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.editor.ACTION_TEXT
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_END
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_START
import com.jetbrains.edu.python.coursecreator.run.PyCCRunTestsConfigurationProducer

open class PyTaskChecker(task: EduTask, project: Project) : EduTaskCheckerBase(task, project) {

  override fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val producer = RunConfigurationProducer.getInstance(PyCCRunTestsConfigurationProducer::class.java)
    val taskDir = task.getTaskDir(project) ?: return emptyList()
    val testFilePath = task.course.configurator?.testFileName ?: return emptyList()
    val file = taskDir.findFileByRelativePath(testFilePath) ?: return emptyList()
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
    val context = ConfigurationContext(psiFile)
    val configurationFromContext = producer.findOrCreateConfigurationFromContext(context)
    return listOfNotNull(configurationFromContext?.configurationSettings)
  }

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (!task.isValid(project)) {
      return CheckResult(CheckStatus.Unchecked,
                         BROKEN_SOLUTION_ERROR_TEXT_START + ACTION_TEXT + BROKEN_SOLUTION_ERROR_TEXT_END)
    }
    return super.check(indicator)
  }

  override fun onTaskFailed(message: String, details: String?) {
    super.onTaskFailed(message, details)
    ApplicationManager.getApplication().invokeLater {
      val taskDir = task.getTaskDir(project)
      if (taskDir == null) return@invokeLater
      for ((_, taskFile) in task.taskFiles) {
        if (taskFile.answerPlaceholders.size < 2) {
          continue
        }
        val course = task.lesson.course
        if (course.isStudy) {
          runUndoTransparentWriteAction {
            PySmartChecker.runSmartTestProcess(taskDir, PyTestRunner(taskDir), taskFile, project)
          }
        }
      }
      CheckUtils.navigateToFailedPlaceholder(EduState(EduUtils.getSelectedEduEditor(project)), task, taskDir, project)
    }
  }

  companion object {
    private val LOG = Logger.getInstance(PyTaskChecker::class.java)
  }
}
