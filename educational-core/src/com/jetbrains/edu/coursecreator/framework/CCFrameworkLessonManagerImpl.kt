package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.*
import com.jetbrains.edu.learning.framework.impl.State
import com.jetbrains.edu.learning.framework.impl.applyChanges
import com.jetbrains.edu.learning.framework.impl.calculateChanges
import com.jetbrains.edu.learning.framework.impl.chooseConflictResolveStrategy
import com.jetbrains.edu.coursecreator.framework.diff.applyChangesViaMergeDialog
import com.jetbrains.edu.learning.framework.impl.getVFSTaskState
import com.jetbrains.edu.learning.framework.impl.showApplyChangesCanceledNotification
import com.jetbrains.edu.learning.framework.impl.showApplyChangesSuccessNotification
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class CCFrameworkLessonManagerImpl(private val project: Project) : CCFrameworkLessonManager, Disposable {
  var storage: CCFrameworkStorage = createStorage(project)

  override fun propagateChanges(task: Task) {
    require(CCUtils.isCourseCreator(project)) {
      "`propagateChangesCC` should be called only if course in CC mode"
    }
    val lesson = task.lesson
    val startIndex = task.index
    val tasks = lesson.taskList

    // Since the indexes start with 1, then startIndex - 1 is the task from which we start propagation
    for (i in startIndex until tasks.size) {
      // we return if user canceled propagation
      if (!propagateChanges(tasks[i - 1], tasks[i])) {
        showApplyChangesCanceledNotification(project, task.name, tasks[i - 1].name)
        return
      }
      // if everything is ok with propagation, then we save the approved changes from the current task into storage
      saveVFSChangesIntoStorage(tasks[i - 1])
    }
    // save last task manually
    saveVFSChangesIntoStorage(tasks.last())
    showApplyChangesSuccessNotification(project, task.name)
  }

  private fun propagateChanges(
    currentTask: Task,
    targetTask: Task,
  ): Boolean {
    require(CCUtils.isCourseCreator(project)) {
      "`propagateChangesCC` should be called only if course in CC mode"
    }
    val currentTaskDir = currentTask.getDir(project.courseDir)
    if (currentTaskDir == null) {
      LOG.error("Failed to find task directory")
      return false
    }

    val targetTaskDir = targetTask.getDir(project.courseDir)
    if (targetTaskDir == null) {
      LOG.error("Failed to find task directory")
      return false
    }

    val initialCurrentFiles = currentTask.allPropagatableFiles
    val initialTargetFiles = targetTask.allPropagatableFiles

    val previousCurrentState = getStateFromStorage(currentTask)

    val currentState = getVFSTaskState(initialCurrentFiles, currentTaskDir)
    val targetState = getVFSTaskState(initialTargetFiles, targetTaskDir)

    // if the state of the file has not changed from the previous one, then it is useless to push it further
    val intersection = currentState.entries.intersect(previousCurrentState.entries)
    val currentStateChanged = currentState.complement(intersection)
    val previousCurrentStateChanged = previousCurrentState.complement(intersection)
    val targetStateChanged = targetState.complementByKeys(intersection)

    return applyChanges(
      currentTask, targetTask,
      currentStateChanged, previousCurrentStateChanged, targetStateChanged,
      targetTaskDir
    )
  }

  private fun applyChanges(
    currentTask: Task,
    targetTask: Task,
    currentState: State,
    previousCurrentState: State,
    targetState: State,
    taskDir: VirtualFile
  ): Boolean {
    val conflictResolveStrategy = chooseConflictResolveStrategy()

    val (areAllConflictsResolved, resolvedChanges) = conflictResolveStrategy.resolveConflicts(
      currentState,
      previousCurrentState,
      targetState
    )

    // replacing the vfs target task state with a state with resolved conflicts
    val resolvedConflictsState = applyChanges(resolvedChanges, previousCurrentState)
    calculateChanges(targetState, resolvedConflictsState).apply(project, taskDir, targetTask)

    if (!areAllConflictsResolved) {
      val isOk = applyChangesViaMergeDialog(
        project,
        targetTask,
        currentState, resolvedConflictsState, targetState,
        currentTask.name, targetTask.name,
        taskDir,
        // it is necessary for the correct recognition of deleting / adding files
        // because new files could be added / removed from the base state after conflict resolution
        previousCurrentState
      )
      if (!isOk) {
        // if the user canceled the dialog, then we return to the target task state
        val currentVFSState = getVFSTaskState(resolvedConflictsState, taskDir)
        calculateChanges(currentVFSState, targetState).apply(project, taskDir, targetTask)
      }
      return isOk
    }
    return true
  }

  private fun saveVFSChangesIntoStorage(task: Task): UpdatedState {
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return UpdatedState(task.record, emptyMap())
    }
    val currentRecord = task.record
    val initialCurrentFiles = task.allPropagatableFiles
    val currentState = getVFSTaskState(initialCurrentFiles, taskDir)
    val updatedUserChanges = try {
      updateState(currentRecord, currentState)
    }
    catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${task.name}`", e)
      UpdatedState(currentRecord, emptyMap())
    }

    task.record = updatedUserChanges.record
    return updatedUserChanges
  }

  private fun getStateFromStorage(task: Task): State {
    return try {
      storage.getState(task.record)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      emptyMap()
    }
  }

  @Synchronized
  private fun updateState(record: Int, state: State): UpdatedState {
    return try {
      val newRecord = storage.updateState(record, state)
      storage.force()
      UpdatedState(newRecord, state)
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedState(record, emptyMap())
    }
  }

  override fun dispose() {
    Disposer.dispose(storage)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCFrameworkLessonManagerImpl::class.java)

    private fun constructStoragePath(project: Project): Path =
      Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistoryCC", "storage"))

    private fun createStorage(project: Project): CCFrameworkStorage {
      val storageFilePath = constructStoragePath(project)
      return CCFrameworkStorage(storageFilePath)
    }
  }
}

private data class UpdatedState(
  val record: Int,
  val state: State,
)