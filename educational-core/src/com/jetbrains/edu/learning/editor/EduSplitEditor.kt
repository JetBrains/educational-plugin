package com.jetbrains.edu.learning.editor

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.TaskFile
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class EduSplitEditor(
  private val project: Project,
  val mainEditor: EduEditor,
  val secondaryEditor: FileEditor,
  private val secondaryTaskFile: TaskFile
) : EduEditor by mainEditor {

  private var component: JComponent? = null

  override fun getName(): String = "EduSplitEditor"

  override fun getComponent(): JComponent = component ?: createComponent()

  private fun createComponent(): JComponent {
    val splitter = JBSplitter(false, 0.5f, 0.15f, 0.85f)

    splitter.splitterProportionKey = "EduSplitEditor.SplitterProportionKey"
    splitter.firstComponent = EditorComponent(secondaryEditor, secondaryTaskFile).apply {
      label.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          val file = secondaryEditor.file ?: return
          FileEditorManager.getInstance(project).openFile(file, true)
        }
      })
    }
    splitter.secondComponent = EditorComponent(mainEditor, mainEditor.taskFile)
    return JBUI.Panels.simplePanel(splitter).apply {
      component = this
    }
  }

  override fun getFile(): VirtualFile? = mainEditor.file

  override fun getState(level: FileEditorStateLevel): EduEditorState {
    val mainEditorState = mainEditor.getState(level)
    return EduEditorState(mainEditorState.mainEditorState, secondaryEditor.getState(level))
  }

  override fun setState(state: FileEditorState, exactState: Boolean) {
    if (state is EduEditorState) {
      mainEditor.setState(state, exactState)
      state.secondaryEditorState?.also {
        secondaryEditor.setState(it, exactState)
      }
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

  private class EditorComponent(fileEditor: FileEditor, taskFile: TaskFile): JPanel(BorderLayout()) {

    val label: JLabel

    init {
      label = JBLabel(taskFile.task.name).apply {
        border = JBUI.Borders.empty(8)
      }
      add(BorderLayout.NORTH, label)
      add(BorderLayout.CENTER, fileEditor.component)
    }
  }
}
