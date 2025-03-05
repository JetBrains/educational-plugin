package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.elements.TaskCreationInfo
import com.jetbrains.edu.learning.update.elements.TaskDeletionInfo
import com.jetbrains.edu.learning.update.elements.TaskUpdate
import com.jetbrains.edu.learning.update.elements.TaskUpdateInfo

abstract class TaskUpdater(project: Project, lesson: Lesson) : TaskUpdaterBase<Lesson>(project, lesson) {

  override suspend fun collect(localItems: List<Task>, remoteItems: List<Task>): List<TaskUpdate> {
    val updates = mutableListOf<TaskUpdate>()

    val localTasks = localItems.toMutableSet()
    val remoteTasks = remoteItems.toMutableSet()

    while (localTasks.isNotEmpty() || remoteTasks.isNotEmpty()) {
      if (localTasks.isEmpty()) {
        // new tasks
        remoteTasks.forEach { remoteTask ->
          updates.add(TaskCreationInfo(lesson, remoteTask))
        }
        remoteTasks.clear()
      }
      if (remoteTasks.isEmpty()) {
        // tasks to be deleted
        localTasks.forEach { localTask ->
          updates.add(TaskDeletionInfo(localTask))
        }
        localTasks.clear()
      }

      // tasks to be updated
      val localTask = localTasks.firstOrNull() ?: continue
      val remoteTask = remoteTasks.find { it.id == localTask.id }
      if (remoteTask == null) {
        updates.add(TaskDeletionInfo(localTask))
        localTasks.remove(localTask)
      }
      else {
        if (localTask.isOutdated(remoteTask) || localTask.isChanged(remoteTask)) {
          updates.add(TaskUpdateInfo(localTask, remoteTask))
        }
        localTasks.remove(localTask)
        remoteTasks.remove(remoteTask)
      }
    }

    return updates
  }
}