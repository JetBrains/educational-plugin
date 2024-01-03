package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.update.elements.TaskCreationInfo
import com.jetbrains.edu.learning.update.elements.TaskDeletionInfo
import com.jetbrains.edu.learning.update.elements.TaskUpdate
import com.jetbrains.edu.learning.update.elements.TaskUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly

abstract class TaskUpdater(project: Project, private val lesson: Lesson) : StudyItemUpdater<Task, TaskUpdate>(project) {

  suspend fun collect(remoteLesson: Lesson): List<TaskUpdate> = collect(lesson.taskList, remoteLesson.taskList)

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

  @TestOnly
  suspend fun update(remoteLesson: Lesson) = update(lesson.taskList, remoteLesson.taskList)

  private suspend fun Task.isChanged(remoteTask: Task): Boolean {
    val newTaskFiles = remoteTask.taskFiles
    val taskDescriptionText = descriptionText.ifEmpty {
      withContext(Dispatchers.EDT) {
        readAction { getDescriptionFile(project)?.getTextFromTaskTextFile() ?: "" }
      }
    }

    return when {
      name != remoteTask.name -> true
      index != remoteTask.index -> true
      taskFiles.size != newTaskFiles.size -> true
      taskDescriptionText != remoteTask.descriptionText -> true
      javaClass != remoteTask.javaClass -> true
      this is ChoiceTask && remoteTask is ChoiceTask -> {
        choiceOptions != remoteTask.choiceOptions
      }
      this is SortingTask && remoteTask is SortingTask -> {
        options != remoteTask.options
      }
      this is MatchingTask && remoteTask is MatchingTask -> {
        options != remoteTask.options || captions != remoteTask.captions
      }
      this is RemoteEduTask && remoteTask is RemoteEduTask -> {
        checkProfile != remoteTask.checkProfile
      }
      else -> {
        newTaskFiles.any { (newFileName, newTaskFile) ->
          isTaskFileChanged(taskFiles[newFileName] ?: return@any true, newTaskFile)
        }
      }
    }
  }

  private fun isTaskFileChanged(taskFile: TaskFile, newTaskFile: TaskFile): Boolean {
    if (taskFile.text != newTaskFile.text) return true
    val taskFilePlaceholders = taskFile.answerPlaceholders
    val newTaskFilePlaceholders = newTaskFile.answerPlaceholders
    if (taskFilePlaceholders.size != newTaskFilePlaceholders.size) return true
    if (newTaskFilePlaceholders.isNotEmpty()) {
      for ((i, newPlaceholder) in newTaskFilePlaceholders.withIndex()) {
        val existingPlaceholder = taskFilePlaceholders[i]
        if (arePlaceholdersDiffer(existingPlaceholder, newPlaceholder)) return true
      }
    }
    return false
  }

  private fun arePlaceholdersDiffer(placeholder: AnswerPlaceholder, newPlaceholder: AnswerPlaceholder): Boolean =
    newPlaceholder.length != placeholder.initialState.length
    || newPlaceholder.offset != placeholder.initialState.offset
    || newPlaceholder.placeholderText != placeholder.placeholderText
    || newPlaceholder.possibleAnswer != placeholder.possibleAnswer
    || newPlaceholder.placeholderDependency.toString() != placeholder.placeholderDependency.toString()
}