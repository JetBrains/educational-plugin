package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class CCChangeFilePropertyActionBase(private val name: String) : DumbAwareAction(name) {

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

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)?.toList() ?: return
    val configurator = StudyTaskManager.getInstance(project).course?.configurator ?: return

    val affectedFiles = mutableListOf<VirtualFile>()
    val states = mutableListOf<State>()
    val tasks = mutableSetOf<Task>()

    fun collect(files: List<VirtualFile>) {
      for (file in files) {
        if (configurator.excludeFromArchive(project, file)) continue
        val task = EduUtils.getTaskForFile(project, file) ?: return
        if (file.isDirectory) {
          collect(VfsUtil.collectChildrenRecursively(file).filter { !it.isDirectory })
        } else {
          affectedFiles += file
        }

        states += createStateForFile(project, task, file) ?: continue
        tasks += task
      }
    }

    collect(virtualFiles)
    tasks.mapTo(states) { TaskState(it) }

    val action = ChangeFilesPropertyUndoableAction(project, states, tasks, affectedFiles)
    EduUtils.runUndoableAction(project, name, action)
  }

  protected open fun isAvailableForFile(project: Project, file: VirtualFile): Boolean {
    val task = EduUtils.getTaskForFile(project, file) ?: return false
    return if (file.isDirectory) {
      // Recursive check is too expensive for `update` method
      // so we allow this action for directories
      true
    } else {
      isAvailableForSingleFile(project, task, file)
    }
  }

  protected abstract fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean
  protected abstract fun createStateForFile(project: Project, task: Task, file: VirtualFile): State?
}

private class ChangeFilesPropertyUndoableAction(
  private val project: Project,
  private val states: List<State>,
  private val affectedTasks: Collection<Task>,
  files: List<VirtualFile>
) : BasicUndoableAction(*files.toTypedArray()) {

  override fun redo() = doAction { it.changeState(project) }
  override fun undo() = doAction { it.restoreState(project) }

  private inline fun doAction(changeState: (State) -> Unit) {
    states.forEach(changeState)
    ProjectView.getInstance(project).refresh()
    affectedTasks.forEach(YamlFormatSynchronizer::saveItem)
  }

  override fun isGlobal(): Boolean = true
}

interface State {
  fun changeState(project: Project)
  fun restoreState(project: Project)
}

private class TaskState(val task: Task) : State {

  val initialStepikStatus: StepikChangeStatus = task.stepikChangeStatus

  override fun changeState(project: Project) {
    StepikCourseChangeHandler.changed(task)
  }

  override fun restoreState(project: Project) {
    task.stepikChangeStatus = initialStepikStatus
  }
}
