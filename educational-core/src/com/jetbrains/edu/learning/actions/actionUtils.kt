package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.selectedTaskFile


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