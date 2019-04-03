package com.jetbrains.edu.learning.editor

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.isFrameworkTask
import org.jdom.Element

class EduFileEditorProvider : FileEditorProvider, DumbAware {

  // Initialize provider lazily to avoid PicoPluginExtensionInitializationException
  // see https://youtrack.jetbrains.com/issue/EDU-1878
  private val defaultTextEditorProvider: TextEditorProvider by lazy { TextEditorProvider.getInstance() }

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val taskFile = EduUtils.getTaskFile(project, file)
    return taskFile != null && !taskFile.isUserCreated && TextEditorProvider.isTextFile(file)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val taskFile = EduUtils.getTaskFile(project, file) ?: error("Can't find task file for `$file`")
    val task = taskFile.task
    if (task.isFrameworkTask && CCUtils.isCourseCreator(project) &&
        Experiments.isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR) &&
        CCSettings.getInstance().showSplitEditor()) {
      val prevTaskFile = task.lesson.taskList.getOrNull(task.index - 2)?.getTaskFile(taskFile.name)
      val taskDir = prevTaskFile?.task?.getTaskDir(project)
      if (prevTaskFile != null && taskDir != null) {
        val prevTaskVirtualFile = EduUtils.findTaskFileInDir(prevTaskFile, taskDir)
        if (prevTaskVirtualFile != null) {
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
            EduSingleFileEditor(project, file, taskFile),
            prevTaskFileEditor,
            prevTaskFile
          )
        }
      }
    }
    return EduSingleFileEditor(project, file, taskFile)
  }

  override fun disposeEditor(editor: FileEditor) {
    defaultTextEditorProvider.disposeEditor(editor)
  }

  override fun readState(sourceElement: Element, project: Project, file: VirtualFile): FileEditorState =
    EduEditorState.read(sourceElement, project, file) ?: defaultTextEditorProvider.readState(sourceElement, project, file)

  override fun writeState(state: FileEditorState, project: Project, targetElement: Element) {
    if (state is EduEditorState) {
      state.write(project, targetElement)
    } else {
      defaultTextEditorProvider.writeState(state, project, targetElement)
    }
  }

  override fun getEditorTypeId(): String = EDITOR_TYPE_ID
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    private const val EDITOR_TYPE_ID = "StudyEditor"
  }
}
