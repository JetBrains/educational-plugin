package com.jetbrains.edu.coursecreator.framework

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.diff.applyChangesWithMergeDialog
import com.jetbrains.edu.coursecreator.framework.diff.resolveConflicts
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.ext.getRelativePath
import com.jetbrains.edu.learning.courseFormat.ext.visitTasks
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.calculateChanges
import com.jetbrains.edu.learning.framework.impl.getTaskStateFromFiles
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * CCFrameworkLessonManager provides operations for framework lessons for the CC.
 * Also, it is also used as the persistent storage for task records in the framework lesson the for course creator,
 *
 * Record of the task - key in the storage framework for the task.
 * It is needed to get information about the previous state of the task, which is stored in the framework storage.
 * For learner, we store the record in yaml, but this option is not suitable for the CC, since we do not want to clutter a yaml.
 * Therefore, the record is stored for the task creator in this service.
 *
 * Currently, the key is the path to the task folder
 * TODO(use id of the task as a key instead of the path to the task)
*/
@Service(Service.Level.PROJECT)
@State(name = "CCFrameworkLessonManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CCFrameworkLessonManager(
  private val project: Project
) : SimplePersistentStateComponent<CCFrameworkLessonManager.RecordState>(RecordState()), Disposable {
  private var storage: CCFrameworkStorage = createStorage(project)

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
    val currentRecord = getRecord(task)
    if (taskDir == null) {
      LOG.error("Failed to find task directory")
      return UpdatedState(currentRecord, emptyMap())
    }
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

    updateRecord(task, updatedUserChanges.record)
    return updatedUserChanges
  }

  private fun getStateFromStorage(task: Task): FLTaskState {
    return try {
      storage.getState(getRecord(task))
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

  @VisibleForTesting
  fun getRecord(task: Task): Int {
    val key = task.getPathInCourse()
    return state.taskRecords[key] ?: -1
  }

  @VisibleForTesting
  fun updateRecord(task: Task, newRecord: Int) {
    if (task.parent !is FrameworkLesson) return
    val key = task.getPathInCourse()
    state.updateTaskRecord(key, newRecord)
    task.record = newRecord
  }

  fun removeRecords(itemContainer: ItemContainer) = itemContainer.visitTasks { removeRecord(it) }

  fun removeRecord(task: Task) {
    if (task.parent !is FrameworkLesson) return
    val key = task.getPathInCourse()
    state.removeTaskRecord(key)
  }

  fun migrateRecords(studyItem: StudyItem, newName: String) {
    if (studyItem is Course) return
    val oldName = studyItem.name

    val prefixPath = studyItem.parent.getPathInCourse()

    studyItem.visitTasks { task ->
      if (task.parent !is FrameworkLesson) return@visitTasks

      val suffixPath = task.getRelativePath(studyItem)

      val oldPath = listOf(prefixPath, oldName, suffixPath).filter { it.isNotEmpty() }.joinToString(VfsUtilCore.VFS_SEPARATOR)
      val newPath = listOf(prefixPath, newName, suffixPath).filter { it.isNotEmpty() }.joinToString(VfsUtilCore.VFS_SEPARATOR)

      state.migrateTaskRecord(oldPath, newPath)
    }
  }

  @TestOnly
  fun recreateStorageIfNeeded() {
    if (storage.isDisposed) {
      storage = createStorage(project)
    }
  }

  @TestOnly
  fun clearStorage() {
    state.taskRecords.clear()
    storage.closeAndClean()
  }

  @VisibleForTesting
  fun getRecord(path: String): Int? {
    return state.taskRecords[path]
  }

  class RecordState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val taskRecords: MutableMap<String, Int> by map()

    fun updateTaskRecord(key: String, newRecord: Int) {
      taskRecords[key] = newRecord
      incrementModificationCount()
    }

    fun removeTaskRecord(key: String) {
      taskRecords.remove(key)
      incrementModificationCount()
    }

    fun migrateTaskRecord(oldKey: String, newKey: String) {
      val record = taskRecords[oldKey] ?: return
      taskRecords.remove(oldKey)
      taskRecords[newKey] = record
      incrementModificationCount()
    }
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