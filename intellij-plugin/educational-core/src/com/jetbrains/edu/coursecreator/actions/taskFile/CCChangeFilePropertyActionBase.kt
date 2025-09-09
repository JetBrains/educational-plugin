package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.EduActionUtils.runUndoableAction
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import java.util.function.Supplier

abstract class CCChangeFilePropertyActionBase(
  private val name: Supplier<@ActionText String>
) : DumbAwareAction(name) {

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

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)?.toList() ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    val affectedFiles = collectAffectedFiles(project, course, virtualFiles)
    val states = mutableListOf<State>()
    val tasks = mutableSetOf<Task>()
    var outsideTasks = false
    for (file in affectedFiles) {
      val task = file.getContainingTask(project)
      if (task == null) {
        outsideTasks = true
      }
      else {
        tasks += task
      }
      states += createStateForFile(project, task, file) ?: continue
    }

    val action = ChangeFilesPropertyUndoableAction(project, course, states, tasks, outsideTasks, affectedFiles)
    runUndoableAction(project, name.get(), action)
  }

  private fun isAvailableForFile(project: Project, file: VirtualFile): Boolean {
    val task = file.getContainingTask(project)
    return if (file.isDirectory) {
      isAvailableForDirectory(project, task, file)
    }
    else {
      isAvailableForSingleFile(project, task, file)
    }
  }

  // Recursive check is too expensive for `update` method,
  // so we allow this action for directories by default
  protected open fun isAvailableForDirectory(project: Project, task: Task?, directory: VirtualFile): Boolean = true

  protected abstract fun isAvailableForSingleFile(project: Project, task: Task?, file: VirtualFile): Boolean

  protected abstract fun createStateForFile(project: Project, task: Task?, file: VirtualFile): State?

  protected open fun collectAffectedFiles(project: Project, course: Course, files: List<VirtualFile>): List<VirtualFile> {
    val affectedFiles = mutableListOf<VirtualFile>()
    val configurator = course.configurator ?: return emptyList()
    for (file in files) {
      if (configurator.excludeFromArchive(project, file)) continue
      if (file.isDirectory) {
        affectedFiles += collectAffectedFiles(project, course, VfsUtil.collectChildrenRecursively(file).filter { !it.isDirectory })
      }
      else {
        affectedFiles += file
      }
    }
    return affectedFiles
  }
}

private class ChangeFilesPropertyUndoableAction(
  private val project: Project,
  private val course: Course,
  private val states: List<State>,
  private val affectedTasks: Collection<Task>,
  private val outsideTasks: Boolean,
  files: List<VirtualFile>
) : BasicUndoableAction(*files.toTypedArray()) {

  override fun redo() = doAction { it.changeState(project) }
  override fun undo() = doAction { it.restoreState(project) }

  private inline fun doAction(changeState: (State) -> Unit) {
    states.forEach(changeState)
    ProjectView.getInstance(project).refresh()
    affectedTasks.forEach { saveItem(it) }
    if (outsideTasks) {
      saveItem(course)
    }
  }

  override fun isGlobal(): Boolean = true
}

interface State {
  fun changeState(project: Project)
  fun restoreState(project: Project)
}
