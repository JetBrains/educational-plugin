package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.courseFormat.TaskFile

abstract class TaskFileUndoableAction(protected val project: Project, protected val taskFile: TaskFile, protected val editor: Editor) : BasicUndoableAction(editor.document) {
  override fun redo() {
    performRedo()
    updateConfigFiles()
  }

  override fun undo() {
    performUndo()
    updateConfigFiles()
  }

  abstract fun performUndo()

  abstract fun performRedo()

  private fun updateConfigFiles() {
    //invokeLater here is needed because one can't change documents while redo/undo
    ApplicationManager.getApplication().invokeLater {
      if (project.isDisposed) {
        return@invokeLater
      }
      YamlFormatSynchronizer.saveItem(taskFile.task)
    }
  }

}