package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.jetbrains.edu.learning.courseFormat.TaskFile

interface EduEditor : TextEditor {
  var taskFile: TaskFile

  fun startLoading()
  fun stopLoading()

  fun validateTaskFile()

  override fun getState(level: FileEditorStateLevel): EduEditorState
}

const val BROKEN_SOLUTION_ERROR_TEXT_START = "Solution can't be loaded."
const val BROKEN_SOLUTION_ERROR_TEXT_END = " to solve it again"
const val ACTION_TEXT = "Reset task"
