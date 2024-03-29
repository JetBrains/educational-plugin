package com.jetbrains.edu.coursecreator.framework.editor

import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

class EduSplitEditorState(
  val mainEditorState: FileEditorState?,
  val secondaryEditorState: FileEditorState? = null
) : FileEditorState {

  override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean =
    when {
      otherState !is EduSplitEditorState -> false
      otherState.mainEditorState == null -> false
      otherState.secondaryEditorState == null -> false
      else -> mainEditorState?.canBeMergedWith(otherState.mainEditorState, level) != false &&
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EduSplitEditorState

    if (mainEditorState != other.mainEditorState) return false
    if (secondaryEditorState != other.secondaryEditorState) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mainEditorState?.hashCode() ?: 0
    result = 31 * result + (secondaryEditorState?.hashCode() ?: 0)
    return result
  }

  companion object {
    private const val SPLIT_EDITOR_STATE = "EduEditorState"
    private const val MAIN_EDITOR_STATE = "MainEditorState"
    private const val SECONDARY_EDITOR_STATE = "SecondaryEditorState"

    fun read(sourceElement: Element, project: Project, file: VirtualFile): EduSplitEditorState? {
      val splitEditorState = sourceElement.getChild(SPLIT_EDITOR_STATE) ?: return null
      val editorProvider = TextEditorProvider.getInstance()
      val mainEditorState = splitEditorState.getChild(MAIN_EDITOR_STATE)?.let {
        editorProvider.readState(it, project, file)
      }
      val secondaryEditorState = splitEditorState.getChild(SECONDARY_EDITOR_STATE)?.let {
        editorProvider.readState(it, project, file)
      }
      return EduSplitEditorState(mainEditorState, secondaryEditorState)
    }
  }
}
