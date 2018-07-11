package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class TaskUndoableAction(@JvmField protected val task: Task, @JvmField val file: VirtualFile) : BasicUndoableAction(file) {
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
    ApplicationManager.getApplication().invokeLater { YamlFormatSynchronizer.saveItem(task) }
  }
}