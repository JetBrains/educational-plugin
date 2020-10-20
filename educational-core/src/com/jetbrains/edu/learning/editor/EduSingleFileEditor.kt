package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager

/**
 * Implementation of [EduEditor] which has panel with special buttons and task text
 *
 * @see [EduFileEditorProvider]
 */
class EduSingleFileEditor(
  project: Project,
  file: VirtualFile,
  override var taskFile: TaskFile
) : PsiAwareTextEditorImpl(project, file, TextEditorProvider.getInstance()), EduEditor {

  override fun getState(level: FileEditorStateLevel): EduEditorState {
    val state = super.getState(level)
    return EduEditorState(state, null)
  }

  override fun setState(state: FileEditorState, exactState: Boolean) {
    val realState = (state as? EduEditorState)?.mainEditorState ?: state
    super<PsiAwareTextEditorImpl>.setState(realState, exactState)
  }

  override fun startLoading() {
    (editor as EditorEx).isViewer = true
    @Suppress("INACCESSIBLE_TYPE")
    val component = component as JBLoadingPanel
    component.setLoadingText("Loading solution")
    component.startLoading()
  }

  override fun stopLoading() {
    @Suppress("INACCESSIBLE_TYPE")
    (component as JBLoadingPanel).stopLoading()
    (editor as EditorEx).isViewer = false
  }

  override fun selectNotify() {
    super<PsiAwareTextEditorImpl>.selectNotify()
    PlaceholderDependencyManager.updateDependentPlaceholders(myProject, taskFile.task)
  }
}
