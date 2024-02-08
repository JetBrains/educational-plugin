package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile

// BACKCOMPAT: 2023.3. Inline
abstract class EduUndoableAction : BasicUndoableAction {

  private val isGlobal: Boolean

  override fun isGlobal(): Boolean = isGlobal

  constructor(files: List<VirtualFile>, isGlobal: Boolean = false) : super(*files.toTypedArray()) {
    this.isGlobal = isGlobal
  }

  constructor(document: Document, isGlobal: Boolean = false) : super(document) {
    this.isGlobal = isGlobal
  }
}

