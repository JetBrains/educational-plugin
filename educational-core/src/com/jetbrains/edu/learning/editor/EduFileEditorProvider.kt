package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import org.jdom.Element

class EduFileEditorProvider : FileEditorProvider, DumbAware {
  private val defaultTextEditorProvider = TextEditorProvider.getInstance()

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val taskFile = EduUtils.getTaskFile(project, file)
    return taskFile != null && !taskFile.isUserCreated && TextEditorProvider.isTextFile(file)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor = EduEditor(project, file)

  override fun disposeEditor(editor: FileEditor) {
    defaultTextEditorProvider.disposeEditor(editor)
  }

  override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState =
    defaultTextEditorProvider.readState(sourceElement, project, file)

  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
    defaultTextEditorProvider.writeState(state, project, targetElement)
  }

  override fun getEditorTypeId(): String = EDITOR_TYPE_ID
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    const val EDITOR_TYPE_ID = "StudyEditor"
  }
}
