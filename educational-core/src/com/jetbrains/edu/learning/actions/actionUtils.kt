package com.jetbrains.edu.learning.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask


fun Project.getCurrentTask(): Task? {
  return FileEditorManager.getInstance(this).selectedFiles
    .map { it.getContainingTask(this) }
    .firstOrNull { it != null }
}