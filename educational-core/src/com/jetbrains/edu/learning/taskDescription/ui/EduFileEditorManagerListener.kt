package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView.Companion.getInstance

class EduFileEditorManagerListener(private val project: Project) : FileEditorManagerListener {
  override fun selectionChanged(event: FileEditorManagerEvent) {
    val file = event.newFile
    val task = file?.getContainingTask(project) ?: return
    getInstance(project).currentTask = task
  }

  override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
    if (FileEditorManager.getInstance(project).openFiles.isEmpty()) {
      getInstance(project).currentTask = null
    }
  }
}