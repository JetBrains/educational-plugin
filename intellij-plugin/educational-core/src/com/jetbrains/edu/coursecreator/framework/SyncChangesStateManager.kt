package com.jetbrains.edu.coursecreator.framework

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.visitFrameworkLessons
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class SyncChangesStateManager(private val project: Project) : Disposable.Default {
  private val taskFileStateStorage = ConcurrentHashMap<TaskFile, SyncChangesTaskFileState>()
  private val taskStateStorage = ConcurrentHashMap<Task, SyncChangesTaskFileState>()
  private val lessonStateStorage = ConcurrentHashMap<Lesson, SyncChangesTaskFileState>()

  private val dispatcher = MergingUpdateQueue(
    "EduSyncChangesTracker",
    syncChangesQueueDelay,
    true,
    null,
    this,
    null,
    false
  ).setRestartTimerOnAdd(true)

  fun getSyncChangesState(taskFile: TaskFile): SyncChangesTaskFileState? {
    if (!isCCFrameworkLesson(taskFile.task.lesson)) return null
    return taskFileStateStorage[taskFile]
  }

  fun getSyncChangesState(task: Task): SyncChangesTaskFileState? {
    if (!isCCFrameworkLesson(task.lesson)) return null
    return taskStateStorage[task]
  }

  fun getSyncChangesState(lesson: Lesson): SyncChangesTaskFileState? {
    if (!isCCFrameworkLesson(lesson)) return null
    return lessonStateStorage[lesson]
  }

  fun taskFileChanged(taskFile: TaskFile) = queueUpdate(taskFile)

  fun taskFileCreated(taskFile: TaskFile) = processTaskFilesCreated(taskFile.task, listOf(taskFile))

  fun filesDeleted(task: Task, taskFilesNames: List<String>) {
    // state of a current task might change from warning to info after deletion, so recalculate it
    queueUpdate(task, emptyList())

    queueSyncChangesStateForFilesInPrevTask(task, taskFilesNames)
  }

  fun taskDeleted(task: Task) = queueSyncChangesStateForFilesInPrevTask(task, null)

  fun fileMoved(file: VirtualFile, fileInfo: FileInfo.FileInTask, oldDirectoryInfo: FileInfo.FileInTask) {
    val task = fileInfo.task
    val oldTask = oldDirectoryInfo.task
    if (!isCCFrameworkLesson(task.lesson) && !isCCFrameworkLesson(oldTask.lesson)) return

    val (taskFiles, oldPaths) = if (file.isDirectory) {
      collectMovedDataInfoOfDirectory(file, fileInfo, oldDirectoryInfo)
    }
    else {
      collectMovedDataInfoOfSingleFile(file, fileInfo, oldDirectoryInfo)
    }

    if (oldTask.lesson is FrameworkLesson) {
      filesDeleted(oldTask, oldPaths)
    }
    if (task.lesson is FrameworkLesson) {
      processTaskFilesCreated(task, taskFiles)
    }
  }

  fun updateSyncChangesState(lessonContainer: LessonContainer) {
    lessonContainer.visitFrameworkLessons { queueUpdate(it) }
  }

  /**
   * Removes state for given task files (that indicates there are no changes in them)
   * Does not schedule anything but update ProjectView and notifications immediately
   */
  fun removeSyncChangesState(task: Task, taskFiles: List<TaskFile>) {
    if (!isCCFrameworkLesson(task.lesson)) return
    for (taskFile in taskFiles) {
      taskFileStateStorage.remove(taskFile)
    }
    collectSyncChangesState(task)
    collectSyncChangesState(task.lesson)
    refreshUI()
  }

  fun updateSyncChangesState(task: Task) = queueUpdate(task)

  @TestOnly
  fun waitForAllRequestsProcessed() {
    dispatcher.waitForAllExecuted(1, TimeUnit.SECONDS)
  }

  private fun collectSyncChangesState(lesson: Lesson) {
    val state = collectState(lesson.taskList) { taskStateStorage[it] }
    if (state != null) lessonStateStorage[lesson] = state
    else lessonStateStorage.remove(lesson)
  }

  private fun collectSyncChangesState(task: Task) {
    val state = collectState(task.taskFiles.values.toList()) {
      if (shouldUpdateSyncChangesState(it)) {
        taskFileStateStorage[it]
      }
      else {
        null
      }
    }
    if (state != null) taskStateStorage[task] = state
    else taskStateStorage.remove(task)
  }

  /**
   * Collects the SyncChangesTaskFileState based on the provided collect function.
   * The state represents the synchronization status of the files and will be displayed in the project view.
   */
  private fun <T> collectState(items: Iterable<T>, collect: (T) -> SyncChangesTaskFileState?): SyncChangesTaskFileState? {
    var resultState: SyncChangesTaskFileState? = null
    for (item in items) {
      val state = collect(item) ?: continue
      if (state == SyncChangesTaskFileState.WARNING) return SyncChangesTaskFileState.WARNING
      resultState = SyncChangesTaskFileState.INFO
    }
    return resultState
  }

  private fun refreshUI() {
    ProjectView.getInstance(project).refresh()
    EditorNotifications.updateAll()
  }

  // In addition/deletion of files, framework lesson structure might break/restore,
  // so we need to recalculate the state for corresponding task files from a previous task
  // in case when a warning state is added/removed
  private fun processTaskFilesCreated(task: Task, taskFiles: List<TaskFile>) {
    queueUpdate(task, taskFiles)
    queueSyncChangesStateForFilesInPrevTask(task, taskFiles.map { it.name })
  }

  private fun queueUpdate(taskFile: TaskFile) = queueUpdate(taskFile.task, listOf(taskFile))
  private fun queueUpdate(task: Task) = queueUpdate(task, task.taskFiles.values.toList())

  private fun queueUpdate(task: Task, taskFiles: List<TaskFile>) {
    if (!isCCFrameworkLesson(task.lesson)) return
    with(dispatcher) {
      queue(TaskFilesSyncChangesUpdate(task, taskFiles.toSet()))
      queue(TaskSyncChangesUpdate(task))
      queue(LessonSyncChangesUpdate(task.lesson))
      queue(ProjectSyncChangesUpdate())
    }
  }

  private fun queueUpdate(lesson: Lesson) {
    if (!isCCFrameworkLesson(lesson)) return
    with(dispatcher) {
      for (task in lesson.taskList) {
        queue(TaskFilesSyncChangesUpdate(task, task.taskFiles.values.toSet()))
        queue(TaskSyncChangesUpdate(task))
      }
      queue(LessonSyncChangesUpdate(lesson))
      queue(ProjectSyncChangesUpdate())
    }
  }

  /**
   * Collects task files in a moved directory and returns a map of task files with their old paths.
   *
   * @return a map of task files with their old paths
   */
  private fun collectMovedDataInfoOfDirectory(
    file: VirtualFile,
    fileInfo: FileInfo.FileInTask,
    oldDirectoryInfo: FileInfo.FileInTask
  ): MovedDataInfo {
    val task = fileInfo.task
    val taskFiles = mutableListOf<TaskFile>()
    val oldPaths = mutableListOf<String>()
    VfsUtil.visitChildrenRecursively(file, object : VirtualFileVisitor<Any?>(NO_FOLLOW_SYMLINKS) {
      override fun visitFile(childFile: VirtualFile): Boolean {
        if (!childFile.isDirectory) {
          val relativePath = VfsUtil.findRelativePath(file, childFile, VfsUtilCore.VFS_SEPARATOR_CHAR) ?: return true
          var oldPath = file.name + VfsUtilCore.VFS_SEPARATOR_CHAR + relativePath
          if (oldDirectoryInfo.pathInTask.isNotEmpty()) {
            oldPath = oldDirectoryInfo.pathInTask + VfsUtilCore.VFS_SEPARATOR_CHAR + oldPath
          }
          val newPath = fileInfo.pathInTask + VfsUtilCore.VFS_SEPARATOR_CHAR + relativePath
          val taskFile = task.taskFiles[newPath] ?: return true
          taskFiles.add(taskFile)
          oldPaths.add(oldPath)
        }
        return true
      }
    })

    return MovedDataInfo(taskFiles, oldPaths)
  }

  private fun collectMovedDataInfoOfSingleFile(
    file: VirtualFile,
    fileInfo: FileInfo.FileInTask,
    oldDirectoryInfo: FileInfo.FileInTask
  ): MovedDataInfo {
    val oldPath = if (oldDirectoryInfo.pathInTask.isNotEmpty()) {
      oldDirectoryInfo.pathInTask + VfsUtilCore.VFS_SEPARATOR_CHAR + file.name
    }
    else {
      file.name
    }
    val taskFile = fileInfo.task.taskFiles[fileInfo.pathInTask] ?: return MovedDataInfo()
    return MovedDataInfo(taskFile, oldPath)
  }

  private fun isCCFrameworkLesson(lesson: Lesson): Boolean {
    return CCUtils.isCourseCreator(project) && lesson is FrameworkLesson
  }

  // Process a batch of taskFiles in a certain task at once to minimize the number of accesses to the storage
  private fun recalcSyncChangesState(task: Task, taskFiles: List<TaskFile>) {
    for (taskFile in taskFiles) {
      taskFileStateStorage.remove(taskFile)
    }

    val updatableTaskFiles = taskFiles.filter { shouldUpdateSyncChangesState(it) }

    val (warningTaskFiles, otherTaskFiles) = updatableTaskFiles.partition { checkForAbsenceInNextTask(it) }

    for (taskFile in warningTaskFiles) {
      taskFileStateStorage[taskFile] = SyncChangesTaskFileState.WARNING
    }

    val changedTaskFiles = CCFrameworkLessonManager.getInstance(project).getChangedFiles(task)
    val infoTaskFiles = otherTaskFiles.intersect(changedTaskFiles.toSet())

    for (taskFile in infoTaskFiles) {
      taskFileStateStorage[taskFile] = SyncChangesTaskFileState.INFO
    }
  }

  // do not update state for the last framework lesson task and for non-propagatable files (invisible files)
  private fun shouldUpdateSyncChangesState(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    return taskFile.isPropagatable && task.lesson.taskList.last() != task
  }

  // after deletion of files, the framework lesson structure might break,
  // so we need to recalculate state for a corresponding file from a previous task in case when a warning state is added/removed
  private fun queueSyncChangesStateForFilesInPrevTask(task: Task, filterTaskFileNames: List<String>?) {
    val prevTask = task.lesson.taskList.getOrNull(task.index - 2) ?: return
    if (filterTaskFileNames == null) {
      queueUpdate(prevTask)
      return
    }
    val correspondingTaskFiles = prevTask.taskFiles.filter { it.key in filterTaskFileNames }.values.toList()
    queueUpdate(prevTask, correspondingTaskFiles)
  }

  private fun checkForAbsenceInNextTask(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    val nextTask = task.lesson.taskList.getOrNull(task.index) ?: return false
    return taskFile.name !in nextTask.taskFiles
  }

  /**
   * Represents information about task files that have been moved.
   * Contains a list of task files and their corresponding old paths.
   */
  private data class MovedDataInfo(val taskFiles: List<TaskFile> = emptyList(), val oldPaths: List<String> = emptyList()) {
    constructor(taskFile: TaskFile, oldPath: String) : this(listOf(taskFile), listOf(oldPath))
  }


  /**
   * Base class for sync changes updates.
   */
  private sealed class SyncChangesUpdate(priority: Int) : Update(Any(), false, priority)

  /**
   * Class for updating state for a list of task files in a given task
   * High priority, since all task files updates must be executed earlier than study item state updates
   */
  private inner class TaskFilesSyncChangesUpdate(val task: Task, val taskFiles: Set<TaskFile>) : SyncChangesUpdate(HIGH_PRIORITY) {
    override fun canEat(update: Update): Boolean {
      if (super.canEat(update)) return true
      if (update !is TaskFilesSyncChangesUpdate) return false
      return task == update.task && taskFiles.containsAll(update.taskFiles)
    }

    override fun run() {
      recalcSyncChangesState(task, taskFiles.toList())
    }
  }

  /**
   * Base class for updating sync changes state for a given study item
   * Lower priority, since all task files updates must be executed earlier than study item state updates
   */
  private sealed class StudyItemSyncChangesUpdate<T : StudyItem>(priority: Int, val item: T) : SyncChangesUpdate(priority)

  /**
   * Class for updating state for a given task
   */
  private inner class TaskSyncChangesUpdate(task: Task) : StudyItemSyncChangesUpdate<Task>(LOW_PRIORITY, task) {
    override fun canEat(update: Update): Boolean {
      if (super.canEat(update)) return true
      if (update !is TaskSyncChangesUpdate) return false
      return item == update.item
    }

    override fun run() {
      collectSyncChangesState(item)
    }
  }

  /**
   * Class for updating state for a given lesson
   * The priority is lower than [TaskSyncChangesUpdate], since all events must be executed later than [TaskSyncChangesUpdate]
   */
  private inner class LessonSyncChangesUpdate(lesson: Lesson) : StudyItemSyncChangesUpdate<Lesson>(LOW_PRIORITY + 1, lesson) {
    override fun canEat(update: Update): Boolean {
      if (super.canEat(update)) return true
      if (update !is LessonSyncChangesUpdate) return false
      return item == update.item
    }

    override fun run() {
      collectSyncChangesState(item)
    }
  }

  /**
   * Base class for updating project UI for sync changes state
   * Lowest priority, since all other updates must be executed
   */
  private inner class ProjectSyncChangesUpdate : SyncChangesUpdate(LOW_PRIORITY + 2) {
    override fun canEat(update: Update): Boolean {
      if (super.canEat(update)) return true
      return update is ProjectSyncChangesUpdate
    }

    override fun run() {
      refreshUI()
    }
  }

  companion object {
    private val syncChangesQueueDelay = Registry.intValue("edu.course.creator.fl.sync.changes.merging.timespan")

    fun getInstance(project: Project): SyncChangesStateManager = project.service()
  }
}
