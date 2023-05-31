package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.command.undo.UnexpectedUndoException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.selectedTaskFile
import org.jetbrains.annotations.Nls


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

private val LOG = logger<EduUtilsKt>()