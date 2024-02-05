package com.jetbrains.edu.coursecreator.framework

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.diff.applyChangesWithMergeDialog
import com.jetbrains.edu.coursecreator.framework.diff.resolveConflicts
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.calculateChanges
import com.jetbrains.edu.learning.framework.impl.getTaskStateFromFiles
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class CCFrameworkLessonManager(private val project: Project) : Disposable {
  private val storage: CCFrameworkStorage = createStorage(project)

  /**
   * Tries to merge changes from the last saved state of [task] until current state to all subsequent tasks
   * and saves changes for them.
   *
   * If the changes cannot be merged automatically, the merge dialog is displayed.
   *
   * [baseFiles] - Files that will be propagated through, if null then all files will be propagated
   */
  fun propagateChanges(task: Task, baseFiles: List<TaskFile>?) {
    require(CCUtils.isCourseCreator(project)) {
      "`propagateChanges` should be called only if course is in CC mode"
    }
    require(task.parent is FrameworkLesson) {
      "`propagateChanges` should be called only when the task is in the framework lesson"
    }
    val lesson = task.lesson
    val startIndex = task.index
    val tasks = lesson.taskList

    val baseFilesNames = baseFiles?.map { it.name }

    // Since the indexes start with 1, then startIndex - 1 is the task from which we start propagation
    for (i in startIndex until tasks.size) {
      // we return if user canceled propagation
      if (!propagateChanges(tasks[i - 1], tasks[i], baseFilesNames)) {
        showApplyChangesCanceledNotification(project, task.name, tasks[i - 1].name)
        return
      }
      // if everything is ok with propagation, then we save the approved changes from the current task into storage
      saveFileStateIntoStorage(tasks[i - 1], baseFilesNames)
    }
    // save last task manually
    saveFileStateIntoStorage(tasks.last(), baseFilesNames)
    showApplyChangesSuccessNotification(project, task.name)
  }

  /**
   * Saves the current file state of the [task] to the storage and updates the record of the task.
   */
  fun saveCurrentState(task: Task) {
    saveFileStateIntoStorage(task)
  }

  private fun propagateChanges(
    currentTask: Task,
    targetTask: Task,
    baseFilesNames: List<String>?,
  ): Boolean {
    val currentTaskDir = currentTask.getDir(project.courseDir) ?: error("Failed to find task directory")
    val targetTaskDir = targetTask.getDir(project.courseDir) ?: error("Failed to find task directory")

    val initialCurrentFiles = calcInitialFiles(currentTask, baseFilesNames)
    val initialTargetFiles = calcInitialFiles(targetTask, baseFilesNames)

    val initialBaseState = getStateFromStorage(currentTask)

    val currentState = getTaskStateFromFiles(initialCurrentFiles, currentTaskDir)
    val targetState = getTaskStateFromFiles(initialTargetFiles, targetTaskDir)

    return applyChanges(currentTask, targetTask, currentState, initialBaseState, targetState, targetTaskDir)
  }

  private fun applyChanges(
    currentTask: Task,
    targetTask: Task,
    currentState: FLTaskState,
    initialBaseState: FLTaskState,
    targetState: FLTaskState,
    taskDir: VirtualFile
  ): Boolean {
    // Try to resolve some changes automatically and apply them to previousCurrentState
    val (conflictFiles, resolvedChangesState) = resolveConflicts(project, currentState, initialBaseState, targetState)

    // if all changes were resolved, then we can apply changes into targetTask
    if (conflictFiles.isEmpty()) {
      calculateChanges(targetState, resolvedChangesState).apply(project, taskDir, targetTask)
      return true
    }

    // If not, then we have to show the merge dialog so the user can resolve the conflicts manually.
    val finalState = applyChangesWithMergeDialog(
      project,
      currentTask,
      targetTask,
      conflictFiles,
      currentState, resolvedChangesState, targetState,
      // it is necessary for the correct recognition of deleting / adding files
      // because new files could be added / removed from the base state after conflict resolution
      initialBaseState
    )

    if (finalState == null) {
      return false
    }

    calculateChanges(targetState, finalState).apply(project, taskDir, targetTask)
    return true
  }

  private fun saveFileStateIntoStorage(task: Task, baseFilesNames: List<String>? = null): UpdatedState {
    val taskDir = task.getDir(project.courseDir)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return UpdatedState(task.record, emptyMap())
    }
    val currentRecord = task.record
    val initialCurrentFiles = calcInitialFiles(task, baseFilesNames)
    val currentState = getTaskStateFromFiles(initialCurrentFiles, taskDir).toMutableMap()

    // if the file is not in baseFiles, then we should not change it's content in storage
    if (baseFilesNames != null) {
      val baseState = getStateFromStorage(task)
      for ((name, content) in baseState) {
        if (name !in baseFilesNames) {
          currentState[name] = content
        }
      }
    }

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

  private fun calcInitialFiles(task: Task, baseFilesNames: List<String>?): Set<String> {
    return baseFilesNames?.intersect(task.allPropagatableFiles) ?: task.allPropagatableFiles
  }

  // we propagate only visible files
  private val Task.allPropagatableFiles: Set<String>
    get() = taskFiles.filterValues { it.isVisible }.keys

  private fun showApplyChangesCanceledNotification(project: Project, startTaskName: String, cancelledTaskName: String) {
    val notification = Notification(
      "JetBrains Academy",
      EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Notification.cancel.title"),
      EduCoreBundle.message(
        "action.Educational.Educator.SyncChangesWithNextTasks.Notification.cancel.description",
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
      EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Notification.success.title"),
      EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Notification.success.description", startTaskName),
      NotificationType.INFORMATION
    )
    Notifications.Bus.notify(notification, project)
  }

  companion object {
    private val LOG = logger<CCFrameworkLessonManager>()

    fun getInstance(project: Project): CCFrameworkLessonManager = project.service()

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