package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.isFrameworkTask
import org.jdom.Element

class EduFileEditorProvider : FileEditorProvider, DumbAware {
  private val defaultTextEditorProvider = TextEditorProvider.getInstance()

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val taskFile = EduUtils.getTaskFile(project, file)
    return taskFile != null && !taskFile.isUserCreated && TextEditorProvider.isTextFile(file)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val taskFile = EduUtils.getTaskFile(project, file) ?: error("Can't find task file for `$file`")
    val task = taskFile.task
    if (task.isFrameworkTask && CCUtils.isCourseCreator(project)) {
      val prevTaskFile = task.lesson.getTaskList().getOrNull(task.index - 2)?.getTaskFile(taskFile.name)
      val taskDir = prevTaskFile?.task?.getTaskDir(project)
      if (prevTaskFile != null && taskDir != null) {
        val prevTaskVirtualFile = EduUtils.findTaskFileInDir(prevTaskFile, taskDir)
        if (prevTaskVirtualFile != null) {
          val previousTaskFileEditor = defaultTextEditorProvider.createEditor(project, prevTaskVirtualFile)
          if (previousTaskFileEditor is TextEditor) {
            val editor = previousTaskFileEditor.editor as? EditorEx
            if (editor != null) {
              editor.isViewer = true
              editor.setCaretVisible(false)
              editor.setCaretEnabled(false)
            }
          }
          return EduSplitEditor(
            project,
            EduSplitEditor.EditorData(EduSingleFileEditor(project, file), taskFile),
            EduSplitEditor.EditorData(previousTaskFileEditor, prevTaskFile)
          )
        }
      }
    }
    return EduSingleFileEditor(project, file)
  }

  override fun disposeEditor(editor: FileEditor) {
    defaultTextEditorProvider.disposeEditor(editor)
  }

  override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState {
    val splitEditorState = sourceElement.getChild(SPLIT_EDITOR_STATE)
    return if (splitEditorState != null) {
      val mainEditorState = splitEditorState.getChild(MAIN_EDITOR_STATE)?.let {
        defaultTextEditorProvider.readState(it, project, file)
      }
      val secondaryEditorState = splitEditorState.getChild(SECONDARY_EDITOR_STATE)?.let {
        defaultTextEditorProvider.readState(it, project, file)
      }
      EduSplitEditor.EduSplitEditorState(mainEditorState, secondaryEditorState)
    } else {
      defaultTextEditorProvider.readState(sourceElement, project, file)
    }
  }

  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
    if (state is EduSplitEditor.EduSplitEditorState) {
      val splitEditorState = Element(SPLIT_EDITOR_STATE)
      if (state.mainEditorState != null) {
        val mainEditorState = Element(MAIN_EDITOR_STATE)
        defaultTextEditorProvider.writeState(state.mainEditorState, project, mainEditorState)
        splitEditorState.addContent(mainEditorState)
      }
      if (state.secondaryEditorState != null) {
        val secondaryEditorState = Element(SECONDARY_EDITOR_STATE)
        defaultTextEditorProvider.writeState(state.secondaryEditorState, project, secondaryEditorState)
        splitEditorState.addContent(secondaryEditorState)
      }
      targetElement.addContent(splitEditorState)
    } else {
      defaultTextEditorProvider.writeState(state, project, targetElement)
    }
  }

  override fun getEditorTypeId(): String = EDITOR_TYPE_ID
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    private const val EDITOR_TYPE_ID = "StudyEditor"

    private const val SPLIT_EDITOR_STATE = "EduSplitEditorState"
    private const val MAIN_EDITOR_STATE = "MainEditorState"
    private const val SECONDARY_EDITOR_STATE = "SecondaryEditorState"
  }
}
