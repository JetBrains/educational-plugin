package com.jetbrains.edu.learning.editor

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class EduSplitEditor(
  private val project: Project,
  private val mainEditorData: EditorData<EduEditor>,
  private val secondaryEditorData: EditorData<FileEditor>
) : EduEditor by mainEditorData.editor {

  private var component: JComponent? = null

  private val mainEditor = mainEditorData.editor
  private val secondaryEditor = secondaryEditorData.editor

  override fun getName(): String = "EduSplitEditor"

  override fun getComponent(): JComponent = component ?: createComponent()

  private fun createComponent(): JComponent {
    val splitter = JBSplitter(false, 0.5f, 0.15f, 0.85f)

    splitter.splitterProportionKey = "EduSplitEditor.SplitterProportionKey"
    splitter.firstComponent = EditorComponent(secondaryEditorData).apply {
      label.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          val file = secondaryEditorData.taskFile.getVirtualFile(project) ?: return
          FileEditorManager.getInstance(project).openFile(file, true)
        }
      })
    }
    splitter.secondComponent = EditorComponent(mainEditorData)
    return JBUI.Panels.simplePanel(splitter).apply {
      component = this
    }
  }

  override fun getState(level: FileEditorStateLevel): FileEditorState =
    EduSplitEditorState(mainEditor.getState(level), secondaryEditor.getState(level))

  override fun setState(state: FileEditorState) {
    if (state is EduSplitEditorState) {
      state.mainEditorState?.also(mainEditor::setState)
      state.secondaryEditorState?.also(secondaryEditor::setState)
    }
  }

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    mainEditor.addPropertyChangeListener(listener)
    secondaryEditor.addPropertyChangeListener(listener)
  }

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    mainEditor.removePropertyChangeListener(listener)
    secondaryEditor.removePropertyChangeListener(listener)
  }

  override fun getStructureViewBuilder(): StructureViewBuilder? = mainEditor.structureViewBuilder

  override fun isModified(): Boolean = mainEditor.isModified && secondaryEditor.isModified
  override fun isValid(): Boolean = mainEditor.isValid && secondaryEditor.isValid

  override fun selectNotify() {
    mainEditor.selectNotify()
    secondaryEditor.selectNotify()
  }

  override fun deselectNotify() {
    mainEditor.deselectNotify()
    secondaryEditor.deselectNotify()
  }

  override fun dispose() {
    Disposer.dispose(mainEditor)
    Disposer.dispose(secondaryEditor)
  }

  data class EditorData<T : FileEditor>(
    val editor: T,
    val taskFile: TaskFile
  )

  class EduSplitEditorState(
    val mainEditorState: FileEditorState?,
    val secondaryEditorState: FileEditorState?
  ) : FileEditorState {

    override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel): Boolean {
      return otherState is EduSplitEditorState &&
             mainEditorState?.canBeMergedWith(otherState.mainEditorState, level) != false &&
             secondaryEditorState?.canBeMergedWith(otherState.secondaryEditorState, level) != false
    }
  }

  private class EditorComponent(editorData: EditorData<out FileEditor>): JPanel(BorderLayout()) {

    val label: JLabel

    init {
      label = JBLabel(editorData.taskFile.task.name).apply {
        border = JBUI.Borders.empty(8)
      }
      add(BorderLayout.NORTH, label)
      add(BorderLayout.CENTER, editorData.editor.component)
    }
  }
}
