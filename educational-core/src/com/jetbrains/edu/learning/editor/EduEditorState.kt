package com.jetbrains.edu.learning.editor

import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class EduEditorState(
  val mainEditorState: FileEditorState?,
  val secondaryEditorState: FileEditorState? = null
) : FileEditorState {

  override fun canBeMergedWith(otherState: FileEditorState?, level: FileEditorStateLevel): Boolean {
    return otherState is EduEditorState &&
           mainEditorState?.canBeMergedWith(otherState.mainEditorState, level) != false &&
           secondaryEditorState?.canBeMergedWith(otherState.secondaryEditorState, level) != false
  }

  fun write(project: Project, targetElement: Element) {
    val editorProvider = TextEditorProvider.getInstance()
    val splitEditorState = Element(SPLIT_EDITOR_STATE)
    if (mainEditorState != null) {
      val mainEditorStateElement = Element(MAIN_EDITOR_STATE)
      editorProvider.writeState(mainEditorState, project, mainEditorStateElement)
      splitEditorState.addContent(mainEditorStateElement)
    }
    if (secondaryEditorState != null) {
      val secondaryEditorStateElement = Element(SECONDARY_EDITOR_STATE)
      editorProvider.writeState(secondaryEditorState, project, secondaryEditorStateElement)
      splitEditorState.addContent(secondaryEditorStateElement)
    }
    targetElement.addContent(splitEditorState)
  }

  companion object {
    private const val SPLIT_EDITOR_STATE = "EduEditorState"
    private const val MAIN_EDITOR_STATE = "MainEditorState"
    private const val SECONDARY_EDITOR_STATE = "SecondaryEditorState"

    @JvmStatic
    fun read(sourceElement: Element, project: Project, file: VirtualFile): EduEditorState? {
      val splitEditorState = sourceElement.getChild(SPLIT_EDITOR_STATE) ?: return null
      val editorProvider = TextEditorProvider.getInstance()
      val mainEditorState = splitEditorState.getChild(MAIN_EDITOR_STATE)?.let {
        editorProvider.readState(it, project, file)
      }
      val secondaryEditorState = splitEditorState.getChild(SECONDARY_EDITOR_STATE)?.let {
        editorProvider.readState(it, project, file)
      }
      return EduEditorState(mainEditorState, secondaryEditorState)
    }
  }
}
