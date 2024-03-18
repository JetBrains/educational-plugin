package com.jetbrains.edu.coursecreator.framework

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.SyncChangesTaskFileState
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.visitFrameworkLessons
import javax.swing.Icon

fun getSyncChangesIcon(taskFile: TaskFile): Icon? = when (taskFile.syncChangesIcon) {
  SyncChangesTaskFileState.NONE -> null
  SyncChangesTaskFileState.INFO -> AllIcons.General.Information
  SyncChangesTaskFileState.WARNING -> AllIcons.General.Warning
}

// Process a batch of taskFiles in a certain task at once to minimize the number of accesses to the storage
private fun updateSyncChangesIcon(project: Project, taskFiles: List<TaskFile>) {
  val task = taskFiles.firstOrNull()?.task ?: return
  if (task.lesson !is FrameworkLesson) return

  for (taskFile in taskFiles) {
    taskFile.syncChangesIcon = SyncChangesTaskFileState.NONE
  }

  val possibleTaskFiles = taskFiles.filter { canShowIconSyncChangesIcon(it) }

  val (taskFilesWithWarningIcon, taskFilesWoutWarningIcon) = possibleTaskFiles.partition { checkForAbsenceInNextTask(it) }

  for (taskFile in taskFilesWithWarningIcon) {
    taskFile.syncChangesIcon = SyncChangesTaskFileState.WARNING
  }

  val changedTaskFiles = CCFrameworkLessonManager.getInstance(project).filterChangedFiles(taskFilesWoutWarningIcon)

  for (taskFile in changedTaskFiles) {
    taskFile.syncChangesIcon = SyncChangesTaskFileState.INFO
  }
}

fun updateSyncChangesIcon(project: Project, taskFile: TaskFile) = updateSyncChangesIcon(project, listOf(taskFile))

// do not show icons for last framework lesson task and for non-propagatable files (visible files)
private fun canShowIconSyncChangesIcon(taskFile: TaskFile): Boolean {
  val task = taskFile.task
  return taskFile.isVisible && task.lesson.taskList.last() != task
}

fun updateSyncChangesIcons(project: Project, task: Task) = updateSyncChangesIcon(project, task.taskFiles.values.toList())

fun updateSyncChangesIcons(project: Project, lesson: FrameworkLesson) {
  lesson.visitTasks {
    updateSyncChangesIcons(project, it)
  }
}

fun updateSyncChangesIcons(project: Project, lessonContainer: LessonContainer) {
  lessonContainer.visitFrameworkLessons {
    updateSyncChangesIcons(project, it)
  }
}

// after deletion of files, framework lesson structure might break,
// so we need to recalculate icons for a corresponding files from a previous task in case when additional warning icons are added
fun recalcSyncChangesIconForFilesInPrevTask(project: Project, task: Task, taskFileNames: List<String>) {
  val prevTask = task.lesson.taskList.getOrNull(task.index - 2) ?: return
  val correspondingTaskFiles = prevTask.taskFiles.filter { it.key in taskFileNames }.values.toList()
  updateSyncChangesIcon(project, correspondingTaskFiles)
}

fun recalcSyncChangesIconForFileInPrevTask(project: Project, task: Task, path: String) {
  recalcSyncChangesIconForFilesInPrevTask(project, task, listOf(path))
}

// after deletion of a task, framework lesson structure might break/restore,
// so we need to recalculate icons for task files from a previous task in case when additional warning icon is added/removed
fun recalcSyncChangesIconForFilesInPrevTask(project: Project, task: Task) {
  val prevTask = task.lesson.taskList.getOrNull(task.index - 2) ?: return
  updateSyncChangesIcon(project, prevTask.taskFiles.values.toList())
}

private fun checkForAbsenceInNextTask(taskFile: TaskFile): Boolean {
  val task = taskFile.task
  val nextTask = task.lesson.taskList.getOrNull(task.index) ?: return false
  return taskFile.name !in nextTask.taskFiles
}

