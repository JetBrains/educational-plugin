package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiUtilCore
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.EduSingleFileEditor
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToFirstFailedAnswerPlaceholder
import com.jetbrains.edu.learning.runReadActionInSmartMode

object CheckUtils {
  const val STUDY_PREFIX = "#educational_plugin"
  const val CONGRATULATIONS = "Congratulations!"
  const val TEST_OK = "test OK"
  const val TEST_FAILED = "FAILED + "
  const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  val COMPILATION_ERRORS = listOf("Compilation failed", "Compilation error")
  const val COMPILATION_FAILED_MESSAGE = "Compilation Failed"
  const val NOT_RUNNABLE_MESSAGE = "Solution isn't runnable"
  const val LOGIN_NEEDED_MESSAGE = "Please, login to Stepik to check the task"
  const val FAILED_TO_CHECK_MESSAGE = "Failed to launch checking"
  const val SYNTAX_ERROR_MESSAGE = "Syntax Error"
  val ERRORS = listOf(COMPILATION_FAILED_MESSAGE, FAILED_TO_CHECK_MESSAGE, SYNTAX_ERROR_MESSAGE)

  fun navigateToFailedPlaceholder(eduState: EduState, task: Task, taskDir: VirtualFile, project: Project) {
    val selectedTaskFile = eduState.taskFile ?: return
    var editor = eduState.editor
    var taskFileToNavigate = selectedTaskFile
    var fileToNavigate = eduState.virtualFile
    val studyTaskManager = StudyTaskManager.getInstance(project)
    if (!studyTaskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for ((_, taskFile) in task.taskFiles) {
        if (studyTaskManager.hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile
          val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
          val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
          if (fileEditor is EduSingleFileEditor) {
            editor = fileEditor.editor
          }
          fileToNavigate = virtualFile
          break
        }
      }
    }
    if (fileToNavigate != null) {
      FileEditorManager.getInstance(project).openFile(fileToNavigate, true)
    }
    if (editor == null) {
      return
    }
    ApplicationManager.getApplication().invokeLater {
      IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }
    navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate)
  }

  fun flushWindows(task: Task, taskDir: VirtualFile) {
    for ((_, taskFile) in task.taskFiles) {
      val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
      EduUtils.flushWindows(taskFile, virtualFile)
    }
  }

  fun createDefaultRunConfiguration(project: Project): RunnerAndConfigurationSettings? {
    return runReadAction {
      val editor = EduUtils.getSelectedEditor(project) ?: return@runReadAction null
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return@runReadAction null
      ConfigurationContext(psiFile).configuration
    }
  }

  fun hasCompilationErrors(processOutput: ProcessOutput): Boolean {
    for (error in COMPILATION_ERRORS) {
      if (processOutput.stderr.contains(error)) return true
    }
    return false
  }

  fun postProcessOutput(output: String): String {
    return output.replace(System.getProperty("line.separator"), "\n").removeSuffix("\n")
  }

  fun createRunConfiguration(project: Project, taskFile: VirtualFile?): RunnerAndConfigurationSettings? {
    return runReadActionInSmartMode(project) {
      val item = PsiUtilCore.findFileSystemItem(project, taskFile)
      if (item == null) null else ConfigurationContext(item).configuration
    }
  }
}
