package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.command.undo.UnexpectedUndoException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.selectedTaskFile
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object EduActionUtils {
  fun getAction(@NonNls id: String): AnAction {
    return ActionManager.getInstance().getAction(id) ?: error("Can not find action by id $id")
  }

  fun showFakeProgress(indicator: ProgressIndicator) {
    if (!isUnitTestMode) {
      checkIsBackgroundThread()
    }
    indicator.isIndeterminate = false
    indicator.fraction = 0.01
    try {
      while (indicator.isRunning) {
        Thread.sleep(1000)
        val fraction = indicator.fraction
        indicator.fraction = fraction + (1 - fraction) * 0.2
      }
    }
    catch (ignore: InterruptedException) {
      // if we remove catch block, exception will die inside pooled thread and logged, but this method can be used somewhere else
    }
  }

  fun Project.getCurrentTask(): Task? {
    return FileEditorManager.getInstance(this).selectedFiles
      .map { it.getContainingTask(this) }
      .firstOrNull { it != null }
  }

  fun updateAction(e: AnActionEvent) {
    e.presentation.isEnabled = false
    val project = e.project ?: return
    project.selectedTaskFile ?: return
    e.presentation.isEnabledAndVisible = true
  }

  fun runUndoableAction(
    project: Project,
    @Nls(capitalization = Nls.Capitalization.Title) name: String?,
    action: UndoableAction
  ) {
    runUndoableAction(project, name, action, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
  }

  fun runUndoableAction(
    project: Project,
    name: @NlsContexts.Command String?,
    action: UndoableAction,
    confirmationPolicy: UndoConfirmationPolicy
  ) {
    try {
      WriteCommandAction.writeCommandAction(project)
        .withName(name)
        .withUndoConfirmationPolicy(confirmationPolicy)
        .run<UnexpectedUndoException> {
          action.redo()
          UndoManager.getInstance(project).undoableActionPerformed(action)
        }
    }
    catch (e: UnexpectedUndoException) {
      LOG.error(e)
    }
  }

  fun <T> waitAndDispatchInvocationEvents(future: Future<T>) {
    if (!isUnitTestMode) {
      LOG.error("`waitAndDispatchInvocationEvents` should be invoked only in unit tests")
    }
    while (true) {
      try {
        UIUtil.dispatchAllInvocationEvents()
        future[10, TimeUnit.MILLISECONDS]
        return
      }
      catch (e: InterruptedException) {
        throw RuntimeException(e)
      }
      catch (e: ExecutionException) {
        throw RuntimeException(e)
      }
      catch (ignored: TimeoutException) {
      }
    }
  }

  @RequiresEdt
  fun Project.closeFileEditor(e: AnActionEvent) {
    val fileEditorManager = FileEditorManager.getInstance(this)
    val fileEditor = e.getData(PlatformDataKeys.FILE_EDITOR) ?: return
    fileEditorManager.closeFile(fileEditor.file)
  }

  private val LOG = logger<EduActionUtils>()
}
