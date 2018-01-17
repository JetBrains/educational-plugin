package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.configuration.CourseChangeHandler
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class TaskUndoableAction(protected val task: Task, protected val file: VirtualFile) : BasicUndoableAction(file) {
  abstract fun performUndo()

  abstract fun performRedo()

  override fun redo() {
    performRedo()
    updateTask()
  }

  override fun undo() {
    performUndo()
    updateTask()
  }

  override fun isGlobal() = true

  private fun updateTask() {
    //invokeLater here is needed because one can't change documents while redo/undo
    ApplicationManager.getApplication().invokeLater { CourseChangeHandler.taskChanged(task) }
  }
}