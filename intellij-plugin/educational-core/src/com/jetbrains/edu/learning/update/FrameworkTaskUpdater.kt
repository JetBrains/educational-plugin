package com.jetbrains.edu.learning.update

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.elements.TaskCreationInfo
import com.jetbrains.edu.learning.update.elements.TaskDeletionInfo
import com.jetbrains.edu.learning.update.elements.TaskUpdate
import com.jetbrains.edu.learning.update.elements.TaskUpdateInfo
import kotlin.math.min

abstract class FrameworkTaskUpdater(project: Project, lesson: FrameworkLesson) : TaskUpdaterBase<FrameworkLesson>(project, lesson) {

  override suspend fun collect(localItems: List<Task>, remoteItems: List<Task>): List<TaskUpdate> {
    // The first tasks in the lists must be the same
    if (!oneIsPrefixOfAnother(localItems, remoteItems)) {
      thisLogger().warn("Failed to update framework lesson \"${lesson.name}\" (id=${lesson.id}): old and new tasks do not match")
      return emptyList()
    }

    val prefixLength = min(localItems.size, remoteItems.size)

    val result = mutableListOf<TaskUpdate>()

    // deleted tasks
    if (localItems.size > prefixLength) {
      for (deletedTask in localItems.subList(prefixLength, localItems.size)) {
        result.add(TaskDeletionInfo(deletedTask))
      }
    }

    // created tasks
    if (remoteItems.size > prefixLength) {
      for (createdTask in remoteItems.subList(prefixLength, remoteItems.size)) {
        result.add(TaskCreationInfo(lesson, createdTask))
      }
    }

    // modified tasks
    for ((localTask, remoteTask) in localItems.zip(remoteItems)) {
      if (localTask.shouldBeUpdated(remoteTask)) {
        result.add(TaskUpdateInfo(localTask, remoteTask))
      }
    }

    return result
  }

  private fun oneIsPrefixOfAnother(localTasks: List<Task>, remoteTasks: List<Task>): Boolean {
    val prefixTasks = localTasks.zip(remoteTasks)

    return prefixTasks.all { (localTask, remoteTask) -> localTask.canBeUpdatedBy(remoteTask) }
  }

  /**
   * The basic check, whether one task could update another.
   * Used to check whether the case of framework lessons update is supported.
   */
  abstract fun Task.canBeUpdatedBy(remoteTask: Task): Boolean

  /**
   * Given that one task could update another, checks whether the update is actually needed.
   */
  abstract suspend fun Task.shouldBeUpdated(remoteTask: Task): Boolean
}