package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.NewPlaceholderPainter
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class CCMakeVisibleToStudent : CCChangeFileVisibility("Make Visible to Student", true)
class CCHideFromStudent : CCChangeFileVisibility("Hide from Student", false)

abstract class CCChangeFileVisibility(val name: String, val requiredVisibility: Boolean) : DumbAwareAction(name) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)?.toList() ?: return
    val configurator = StudyTaskManager.getInstance(project).course?.configurator ?: return

    val affectedFiles = mutableListOf<VirtualFile>()
    val states = mutableListOf<State>()
    val tasks = mutableSetOf<Task>()

    fun collect(files: List<VirtualFile>) {
      for (file in files) {
        if (configurator.excludeFromArchive(project, file.path)) continue
        val task = EduUtils.getTaskForFile(project, file) ?: return
        if (file.isDirectory) {
          collect(VfsUtil.collectChildrenRecursively(file).filter { !it.isDirectory })
        } else {
          affectedFiles += file
        }
        val taskRelativePath = EduUtils.pathRelativeToTask(project, file)
        val taskFile = task.getTaskFile(taskRelativePath)
        if (taskFile != null) {
          states += TaskFileState(taskFile, file)
          tasks += task
        } else {
          val additionalFile = task.additionalFiles[taskRelativePath]
          if (additionalFile != null) {
            states += AdditionalFileState(additionalFile)
            tasks += task
          }
        }
      }
    }

    collect(virtualFiles)
    tasks.mapTo(states) { TaskState(it) }

    val action = ChangeVisibilityUndoableAction(project, states, tasks, requiredVisibility, affectedFiles)
    EduUtils.runUndoableAction(project, name, action)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    if (project == null || !CCUtils.isCourseCreator(project)) return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext).orEmpty()

    presentation.isEnabledAndVisible = when (virtualFiles.size) {
      0 -> false
      1 -> isAvailableForFile(project, virtualFiles.single())
      else -> virtualFiles.all { isAvailableForFile(project, it) }
    }
  }

  private fun isAvailableForFile(project: Project, file: VirtualFile): Boolean {
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    return if (file.isDirectory) {
      // Recursive check is too expensive for `update` method
      // so we allow this action for directories
      true
    } else {
      val path = EduUtils.pathRelativeToTask(project, file)
      val visibleFile = task.getTaskFile(path) ?: task.additionalFiles[path]
      visibleFile?.isVisible == !requiredVisibility
    }
  }

  private class ChangeVisibilityUndoableAction(
    private val project: Project,
    private val states: List<State>,
    private val affectedTasks: Collection<Task>,
    private val visibility: Boolean,
    files: List<VirtualFile>
  ) : BasicUndoableAction(*files.toTypedArray()) {

    override fun redo() = doAction { it.changeState(project, visibility) }
    override fun undo() = doAction { it.restoreState(project) }

    private inline fun doAction(changeState: (State) -> Unit) {
      states.forEach(changeState)
      ProjectView.getInstance(project).refresh()
      // invokeLater here is needed because one can't change documents while redo/undo
      ApplicationManager.getApplication().invokeLater {
        affectedTasks.forEach(YamlFormatSynchronizer::saveItem)
      }
    }

    override fun isGlobal(): Boolean = true
  }
}

private sealed class State {
  abstract fun changeState(project: Project, isVisible: Boolean)
  abstract fun restoreState(project: Project)
}

private class TaskState(val task: Task) : State() {

  val initialStepikStatus: StepikChangeStatus = task.stepikChangeStatus

  override fun changeState(project: Project, isVisible: Boolean) {
    StepikCourseChangeHandler.changed(task)
  }

  override fun restoreState(project: Project) {
    task.stepikChangeStatus = initialStepikStatus
  }
}

private class AdditionalFileState(val file: AdditionalFile) : State() {

  val initialVisibility: Boolean = file.isVisible

  override fun changeState(project: Project, isVisible: Boolean) {
    file.isVisible = isVisible
  }

  override fun restoreState(project: Project) {
    file.isVisible = initialVisibility
  }
}

private class TaskFileState(
  val taskFile: TaskFile,
  val file: VirtualFile
) : State() {

  val initialVisibility: Boolean = taskFile.isVisible
  val placeholders: List<AnswerPlaceholder> = taskFile.answerPlaceholders

  override fun changeState(project: Project, isVisible: Boolean) {
    taskFile.isVisible = isVisible
    onVisibilityChange(project, taskFile, file, isVisible)
    taskFile.answerPlaceholders = mutableListOf()
  }

  override fun restoreState(project: Project) {
    taskFile.isVisible = initialVisibility
    taskFile.answerPlaceholders = placeholders
    onVisibilityChange(project, taskFile, file, initialVisibility)
  }

  private fun onVisibilityChange(project: Project, taskFile: TaskFile, file: VirtualFile, visibility: Boolean) {
    if (taskFile.answerPlaceholders.isEmpty() || !FileEditorManager.getInstance(project).isFileOpen(file)) return
    if (visibility) {
      showPlaceholders(project, taskFile, file)
    } else {
      hidePlaceholders(project, taskFile, file)
    }
  }

  private fun showPlaceholders(project: Project, taskFile: TaskFile, file: VirtualFile) {
    for (editor in file.editors(project)) {
      EduUtils.drawAllAnswerPlaceholders(editor, taskFile)
    }
  }

  private fun hidePlaceholders(project: Project, taskFile: TaskFile, file: VirtualFile) {
    for (editor in file.editors(project)) {
      for (placeholder in taskFile.answerPlaceholders) {
        NewPlaceholderPainter.removePainter(editor, placeholder)
      }
    }
  }

  private fun VirtualFile.editors(project: Project): List<Editor> {
    return FileEditorManager.getInstance(project).getEditors(this)
      .filterIsInstance<TextEditor>()
      .map { it.editor }
  }
}
