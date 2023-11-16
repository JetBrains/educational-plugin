package com.jetbrains.edu.coursecreator.framework

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.diff.FLConflictResolveStrategy
import com.jetbrains.edu.coursecreator.framework.diff.SimpleConflictResolveStrategy
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.coursecreator.framework.diff.applyChangesWithMergeDialog
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.calculateChanges
import com.jetbrains.edu.learning.framework.impl.getTaskStateFromFiles
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class CCFrameworkLessonManagerImpl(private val project: Project) : CCFrameworkLessonManager, Disposable {
  var storage: CCFrameworkStorage = createStorage(project)

  override fun propagateChanges(task: Task) {
    require(CCUtils.isCourseCreator(project)) {
      "`propagateChanges` should be called only if course in CC mode"
    }
    require(task.parent is FrameworkLesson) {
      "`propagateChanges` should be called only when the task is in the framework lesson"
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
      saveFileStateIntoStorage(tasks[i - 1])
    }
    // save last task manually
    saveFileStateIntoStorage(tasks.last())
    showApplyChangesSuccessNotification(project, task.name)
  }

  override fun saveCurrentState(task: Task) {
    saveFileStateIntoStorage(task)
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

    val currentState = getTaskStateFromFiles(initialCurrentFiles, currentTaskDir)
    val targetState = getTaskStateFromFiles(initialTargetFiles, targetTaskDir)

    return applyChanges(currentTask, targetTask, currentState, previousCurrentState, targetState, targetTaskDir)
  }

  private fun applyChanges(
    currentTask: Task,
    targetTask: Task,
    currentState: FLTaskState,
    previousCurrentState: FLTaskState,
    targetState: FLTaskState,
    taskDir: VirtualFile
  ): Boolean {
    val conflictResolveStrategy = chooseConflictResolveStrategy()

    // Try to resolve some changes automatically and apply them to previousCurrentState
    val (conflictFiles, resolvedChangesState) = conflictResolveStrategy.resolveConflicts(
      currentState,
      previousCurrentState,
      targetState
    )

    // if all changes were resolved, then we can apply changes into targetTask
    if (conflictFiles.isEmpty()) {
      calculateChanges(targetState, resolvedChangesState).apply(project, taskDir, targetTask)
      return true
    }

    // If not, then we have to show the merge dialog so the user can resolve the conflicts manually.
    // Merge dialog requires that the files that will be shown should be passed as virtual files.
    // So we replace target task files with files from base state with resolved conflicts and we change them in merge dialog
    // TODO(Show merge dialog without changing target task files)
    calculateChanges(targetState, resolvedChangesState).apply(project, taskDir, targetTask)

    val isOk = applyChangesWithMergeDialog(
      project,
      currentTask,
      targetTask,
      conflictFiles,
      currentState, resolvedChangesState, targetState,
      taskDir,
      // it is necessary for the correct recognition of deleting / adding files
      // because new files could be added / removed from the base state after conflict resolution
      previousCurrentState
    )
    if (!isOk) {
      // if the user canceled the dialog, then we return to the target task state
      val currentStateFromFiles = getTaskStateFromFiles(resolvedChangesState.keys, taskDir)
      calculateChanges(currentStateFromFiles, targetState).apply(project, taskDir, targetTask)
    }
    return isOk
  }

  private fun saveFileStateIntoStorage(task: Task): UpdatedState {
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return UpdatedState(task.record, emptyMap())
    }
    val currentRecord = task.record
    val initialCurrentFiles = task.allPropagatableFiles
    val currentState = getTaskStateFromFiles(initialCurrentFiles, taskDir)
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

  private fun getStateFromStorage(task: Task): FLTaskState {
    return try {
      storage.getState(task.record)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      emptyMap()
    }
  }

  @Synchronized
  private fun updateState(record: Int, state: FLTaskState): UpdatedState {
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

  // we propagate only visible files
  private val Task.allPropagatableFiles: Set<String>
    get() = taskFiles.filterValues { it.isVisible }.keys

  private fun showApplyChangesCanceledNotification(project: Project, startTaskName: String, cancelledTaskName: String) {
    val notification = Notification(
      "JetBrains Academy",
      EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.cancel.title"),
      EduCoreBundle.message(
        "action.Educational.Educator.ApplyChangesToNextTasks.Notification.cancel.description",
        startTaskName,
        cancelledTaskName
      ),
      NotificationType.WARNING
    )
    Notifications.Bus.notify(notification, project)
  }

  private fun showApplyChangesSuccessNotification(project: Project, startTaskName: String) {
    val notification = Notification(
      "JetBrains Academy",
      EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.success.title"),
      EduCoreBundle.message("action.Educational.Educator.ApplyChangesToNextTasks.Notification.success.description", startTaskName),
      NotificationType.INFORMATION
    )
    Notifications.Bus.notify(notification, project)
  }

  private fun chooseConflictResolveStrategy(): FLConflictResolveStrategy {
    return SimpleConflictResolveStrategy()
  }

  companion object {
    private val LOG = logger<CCFrameworkLessonManagerImpl>()

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
  val state: FLTaskState,
)