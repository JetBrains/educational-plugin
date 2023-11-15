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
import com.jetbrains.edu.learning.framework.impl.diff.applyChangesViaMergeDialog
import com.jetbrains.edu.learning.framework.impl.getVFSTaskState
import com.jetbrains.edu.learning.framework.impl.showApplyChangesCanceledNotification
import com.jetbrains.edu.learning.framework.impl.showApplyChangesSuccessNotification
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class CCFrameworkLessonManagerImpl(private val project: Project): CCFrameworkLessonManager, Disposable {
  var storage: FrameworkStorage = createStorage(project)

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

    val initialCurrentFiles = currentTask.allFiles
    val initialTargetFiles = targetTask.allFiles

    val previousCurrentState = getTaskStateFromStorage(currentTask)

    val currentUserChanges = getUserChangesFromVFS(initialCurrentFiles, currentTaskDir)
    val currentState = applyChanges(currentUserChanges, initialCurrentFiles)

    val targetState = getTaskStateFromVCS(initialTargetFiles, targetTaskDir)

    // we propagate only visible files
    val currentStateVisible = currentState.split(currentTask).first
    val previousCurrentStateVisible = previousCurrentState.split(currentTask).first
    val targetStateVisible = targetState.split(targetTask).first

    // if the state of the file has not changed from the previous one, then it is useless to push it further
    val intersection = currentStateVisible.entries.intersect(previousCurrentStateVisible.entries)
    val currentStateVisibleChanged = currentStateVisible.complement(intersection)
    val previousCurrentStateVisibleChanged = previousCurrentStateVisible.complement(intersection)
    val targetStateVisibleChanged = targetStateVisible.complementByKeys(intersection)

    return applyChanges(
      currentTask, targetTask,
      currentStateVisibleChanged, previousCurrentStateVisibleChanged, targetStateVisibleChanged,
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

  private fun saveVFSChangesIntoStorage(task: Task): UpdatedUserChanges {
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return UpdatedUserChanges(task.record, UserChanges.empty())
    }
    val currentRecord = task.record
    val initialCurrentFiles = task.allFiles
    val updatedUserChanges = try {
      updateUserChanges(currentRecord, initialCurrentFiles, taskDir)
    }
    catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${task.name}`", e)
      UpdatedUserChanges(currentRecord, UserChanges.empty())
    }

    task.record = updatedUserChanges.record
    return updatedUserChanges
  }

  private fun getTaskStateFromStorage(task: Task): State {
    val initialState = task.allFiles
    val storageChanges = getUserChangesFromStorage(task)
    return applyChanges(storageChanges, initialState)
  }

  private fun getUserChangesFromStorage(task: Task): UserChanges {
    return try {
      storage.getUserChanges(task.record)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      UserChanges.empty()
    }
  }

  @Synchronized
  private fun updateUserChanges(record: Int, changes: UserChanges): UpdatedUserChanges {
    return try {
      val newRecord = storage.updateUserChanges(record, changes)
      storage.force()
      UpdatedUserChanges(newRecord, changes)
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedUserChanges(record, UserChanges.empty())
    }
  }

  override fun dispose() {
    Disposer.dispose(storage)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CCFrameworkLessonManagerImpl::class.java)

    private fun constructStoragePath(project: Project): Path =
      Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistoryCC", "storage"))

    private fun createStorage(project: Project): FrameworkStorage {
      val storageFilePath = constructStoragePath(project)
      return FrameworkStorage(storageFilePath)
    }
  }
}

private data class UpdatedUserChanges(
  val record: Int,
  val changes: UserChanges
)
