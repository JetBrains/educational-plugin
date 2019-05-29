package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.ACTION_TEXT
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_END
import com.jetbrains.edu.learning.editor.BROKEN_SOLUTION_ERROR_TEXT_START
import java.util.concurrent.CountDownLatch

open class PyTaskChecker(task: EduTask, project: Project) : TaskChecker<EduTask>(task, project) {

  override fun check(indicator: ProgressIndicator): CheckResult {
    val taskDir = task.getTaskDir(project)
    if (taskDir == null) {
      LOG.info("taskDir is null for task " + task.name)
      return CheckResult(CheckStatus.Unchecked, "Task is broken")
    }

    if (!task.isValid(project)) {
      return CheckResult(CheckStatus.Unchecked,
                         BROKEN_SOLUTION_ERROR_TEXT_START + ACTION_TEXT + BROKEN_SOLUTION_ERROR_TEXT_END)
    }
    val latch = CountDownLatch(1)
    ApplicationManager.getApplication().invokeLater {
      runWriteAction {
        CheckUtils.flushWindows(task, taskDir)
        latch.countDown()
      }
    }
    val testRunner = PyTestRunner(taskDir)
    try {
      val fileToCheck = getTaskVirtualFile(task, taskDir)
      if (fileToCheck != null) {
        //otherwise answer placeholders might have been not flushed yet
        latch.await()
        val testProcess = testRunner.createCheckProcess(project, fileToCheck.path)
        return getCheckResult(testProcess, testRunner.commandLine.commandLineString)
      }
    }
    catch (e: ExecutionException) {
      LOG.error(e)
    }
    catch (e: InterruptedException) {
      LOG.error(e)
    }

    return CheckResult.FAILED_TO_CHECK
  }

  override fun clearState() {
    ApplicationManager.getApplication().invokeLater {
      val taskDir = task.getTaskDir(project)
      if (taskDir != null) {
        EduUtils.deleteWindowDescriptions(task, taskDir)
      }
    }
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

    private fun getTaskVirtualFile(task: Task, taskDir: VirtualFile): VirtualFile? {
      var firstFile: VirtualFile? = null
      for ((_, taskFile) in task.taskFiles) {
        val file = EduUtils.findTaskFileInDir(taskFile, taskDir)
        if (file == null) {
          LOG.warn(String.format("Can't find virtual file for `%s` task file in `%s` task", taskFile.name, task.name))
          continue
        }
        if (firstFile == null) {
          firstFile = file
        }

        // TODO: Come up with a smarter way how to find correct task file
        val hasNewPlaceholder = taskFile.answerPlaceholders
          .any { p -> p.placeholderDependency == null }
        if (hasNewPlaceholder) return file
      }
      return firstFile
    }

    fun getCheckResult(testProcess: Process, commandLine: String): CheckResult {
      val handler = CapturingProcessHandler(testProcess, null, commandLine)
      val output = if (ProgressManager.getInstance().hasProgressIndicator()) {
        handler.runProcessWithProgressIndicator(ProgressManager.getInstance().progressIndicator)
      }
      else {
        handler.runProcess()
      }
      val stderr = output.stderr
      if (stderr.isNotEmpty() && output.stdout.isEmpty()) {
        LOG.info("#educational $stderr")
        return CheckResult(CheckStatus.Failed, stderr, null, null, false)
      }
      return TestsOutputParser.getCheckResult(output.stdoutLines, false)
    }
  }
}
