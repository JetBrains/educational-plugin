package com.jetbrains.edu.coursecreator.framework.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isFrameworkTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.isFeatureEnabled
import org.jdom.Element

class EduSplitFileEditorProvider : FileEditorProvider, DumbAware {

  // Initialize provider lazily to avoid PicoPluginExtensionInitializationException
  // see https://youtrack.jetbrains.com/issue/EDU-1878
  private val defaultTextEditorProvider: TextEditorProvider by lazy { TextEditorProvider.getInstance() }

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val taskFile = file.getTaskFile(project)
    return taskFile != null && TextEditorProvider.isTextFile(file) && taskFile.createSplitEditor(project)
  }

  private fun TaskFile.createSplitEditor(project: Project): Boolean {
    return task.isFrameworkTask
           && previousTaskFile() != null
           && CCUtils.isCourseCreator(project)
           && isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)
           && CCSettings.getInstance().showSplitEditor
  }

  private fun TaskFile.previousTaskFile(): TaskFile? {
    return task.lesson.taskList.getOrNull(task.index - 2)?.getTaskFile(name)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val taskFile = file.getTaskFile(project) ?: error("Can't find task file for `$file`")
    val prevTaskFile = taskFile.previousTaskFile() ?: error("Can't find previous task file for `$file`")
    val prevTaskVirtualFile = prevTaskFile.getVirtualFile(project) ?: error("VirtualFile for task file `${prevTaskFile.name}`not found")

    val prevTaskFileEditor = defaultTextEditorProvider.createEditor(project, prevTaskVirtualFile)
    if (prevTaskFileEditor is TextEditor) {
      val editor = prevTaskFileEditor.editor as? EditorEx
      if (editor != null) {
        editor.isViewer = true
        editor.setCaretVisible(false)
        editor.setCaretEnabled(false)
      }
    }
    return EduSplitEditor(
      project,
      defaultTextEditorProvider.createEditor(project, file) as TextEditor,
      prevTaskFileEditor,
      taskFile,
      prevTaskFile
    )
  }

  override fun disposeEditor(editor: FileEditor) {
    defaultTextEditorProvider.disposeEditor(editor)
  }

  override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState =
    EduSplitEditorState.read(sourceElement, project, file) ?: defaultTextEditorProvider.readState(sourceElement, project, file)

  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
    if (state is EduSplitEditorState) {
      state.write(project, targetElement)
    }
    else {
      defaultTextEditorProvider.writeState(state, project, targetElement)
    }
  }

  override fun getEditorTypeId(): String = EDITOR_TYPE_ID
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    private const val EDITOR_TYPE_ID = "EduSplitEditor"
  }
}
