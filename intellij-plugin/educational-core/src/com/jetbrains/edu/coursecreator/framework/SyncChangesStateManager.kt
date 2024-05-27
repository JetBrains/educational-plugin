package com.jetbrains.edu.coursecreator.framework

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.visitFrameworkLessons
import com.jetbrains.edu.learning.isFeatureEnabled
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SyncChangesStateManager(private val project: Project) {
  private val stateStorage = ConcurrentHashMap<TaskFile, SyncChangesTaskFileState>()

  fun getSyncChangesState(taskFile: TaskFile): SyncChangesTaskFileState? {
    if (!checkRequirements(taskFile.task.lesson)) return null
    return stateStorage[taskFile]
  }

  fun taskFileChanged(taskFile: TaskFile) {
    if (!checkRequirements(taskFile.task.lesson)) return
    updateSyncChangesState(taskFile.task, listOf(taskFile))
  }

  fun taskFileCreated(taskFile: TaskFile) {
    if (!checkRequirements(taskFile.task.lesson)) return
    processTaskFilesCreated(taskFile.task, listOf(taskFile))
  }

  fun updateSyncChangesState(lessonContainer: LessonContainer) {
    if (!CCUtils.isCourseCreator(project) || !isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)) return
    lessonContainer.visitFrameworkLessons { lesson ->
      lesson.visitTasks {
        updateSyncChangesState(it)
      }
    }
  }

  fun updateSyncChangesState(task: Task) {
    if (!checkRequirements(task.lesson)) return
    updateSyncChangesState(task, task.taskFiles.values.toList())
  }

  // In addition/deletion of files, framework lesson structure might break/restore,
  // so we need to recalculate the state for corresponding task files from a previous task
  // in case when a warning state is added/removed
  private fun processTaskFilesCreated(task: Task, taskFiles: List<TaskFile>) {
    updateSyncChangesState(task, taskFiles)
    recalcSyncChangesStateForFilesInPrevTask(task, taskFiles.map { it.name })
  }

  private fun checkRequirements(lesson: Lesson): Boolean {
    return CCUtils.isCourseCreator(project) && lesson is FrameworkLesson && isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)
  }

  // Process a batch of taskFiles in a certain task at once to minimize the number of accesses to the storage
  private fun updateSyncChangesState(task: Task, taskFiles: List<TaskFile>) {
    for (taskFile in taskFiles) {
      stateStorage.remove(taskFile)
    }

    val updatableTaskFiles = taskFiles.filter { shouldUpdateSyncChangesState(it) }

    val (warningTaskFiles, otherTaskFiles) = updatableTaskFiles.partition { checkForAbsenceInNextTask(it) }

    for (taskFile in warningTaskFiles) {
      stateStorage[taskFile] = SyncChangesTaskFileState.WARNING
    }

    val changedTaskFiles = CCFrameworkLessonManager.getInstance(project).getChangedFiles(task)
    val infoTaskFiles = otherTaskFiles.intersect(changedTaskFiles.toSet())

    for (taskFile in infoTaskFiles) {
      stateStorage[taskFile] = SyncChangesTaskFileState.INFO
    }
    // TODO(refresh only necessary nodes instead of refreshing whole project view tree)
    ProjectView.getInstance(project).refresh()
  }

  // do not update state for the last framework lesson task and for non-propagatable files (invisible files)
  private fun shouldUpdateSyncChangesState(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    return taskFile.isVisible && task.lesson.taskList.last() != task
  }

  // after deletion of files, the framework lesson structure might break,
  // so we need to recalculate state for a corresponding file from a previous task in case when a warning state is added/removed
  private fun recalcSyncChangesStateForFilesInPrevTask(task: Task, filterTaskFileNames: List<String>?) {
    val prevTask = task.lesson.taskList.getOrNull(task.index - 2) ?: return
    val correspondingTaskFiles = if (filterTaskFileNames != null) {
      prevTask.taskFiles.filter { it.key in filterTaskFileNames }
    }
    else {
      prevTask.taskFiles
    }.values.toList()
    updateSyncChangesState(prevTask, correspondingTaskFiles)
  }

  private fun checkForAbsenceInNextTask(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    val nextTask = task.lesson.taskList.getOrNull(task.index) ?: return false
    return taskFile.name !in nextTask.taskFiles
  }

  companion object {
    fun getInstance(project: Project): SyncChangesStateManager = project.service()
  }
}
