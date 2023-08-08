package com.jetbrains.edu.coursecreator.actions.placeholder

import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

abstract class TaskFileUndoableAction(protected val project: Project, protected val taskFile: TaskFile, protected val editor: Editor) : BasicUndoableAction(editor.document) {

  override fun redo() {
    performRedo()
    updateConfigFiles()
  }

  override fun undo() {
    if (performUndo()) {
      updateConfigFiles()
    }
  }

  abstract fun performUndo(): Boolean

  abstract fun performRedo()

  private fun updateConfigFiles() {
    //invokeLater here is needed because one can't change documents while redo/undo
    project.invokeLater {
      YamlFormatSynchronizer.saveItem(taskFile.task)
    }
  }

}