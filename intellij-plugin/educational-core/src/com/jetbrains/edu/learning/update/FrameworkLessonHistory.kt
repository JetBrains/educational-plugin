package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.ext.shouldBePropagated
import com.jetbrains.edu.learning.framework.FrameworkLessonManager

class FrameworkLessonTaskHistory(project: Project, localLesson: FrameworkLesson, remoteLesson: FrameworkLesson) {
  val taskFileHistories: Map<String, FrameworkLessonTaskFileHistory>

  init {
    // collect a set of all task file names from both local and remote lessons
    val localFileNames = localLesson.taskList.flatMap { it.taskFiles.keys }
    val remoteFileNames = remoteLesson.taskList.flatMap { it.taskFiles.keys }
    val allTaskFiles = localFileNames.toSet() + remoteFileNames.toSet()

    taskFileHistories = allTaskFiles.associateWith { fileName ->
      FrameworkLessonTaskFileHistory(
        project,
        localLesson,
        remoteLesson,
        fileName
      )
    }
  }
}

class FrameworkLessonTaskFileHistory(
  private val project: Project,
  localLesson: FrameworkLesson,
  remoteLesson: FrameworkLesson,
  fileName: String
) {

  private val remoteHistory: List<TaskFileStep?> = extractTaskFileHistory(remoteLesson, fileName)
  private val userChanges: List<FileContents?> = extractUserChangesHistory(localLesson, fileName)

  private fun extractTaskFileHistory(lesson: FrameworkLesson, fileName: String): List<TaskFileStep?> =
    lesson.taskList.map { task ->
      val taskFile = task.getTaskFile(fileName)
      taskFile?.let { TaskFileStep(it.contents, it.shouldBePropagated()) }
    }

  private fun extractUserChangesHistory(lesson: FrameworkLesson, fileName: String): List<FileContents?> {
    val flManager = FrameworkLessonManager.getInstance(project)
    return lesson.taskList.map { task ->
      val text = flManager.getTaskState(lesson, task)[fileName]
      val taskFile = task.getTaskFile(fileName)

      if (text == taskFile?.text) {
        null // no modification
      }
      else {
        text?.let { InMemoryTextualContents(it) }
      }
    }
  }

  fun evaluateContents(
    index: Int,
    isTemplateBased: Boolean
  ): FileContents? {
    if (isTemplateBased) {
      return userChanges[index] ?: remoteHistory[index]?.authorContents
    }

    var currentIndex = index

    while (currentIndex >= 0) {
      val currentUserChanges = userChanges[currentIndex]
      if (currentUserChanges != null) return currentUserChanges // user changes are the most important, so we always take them if they present

      val historyStep = remoteHistory[currentIndex]
      if (historyStep == null) {
        // In this case, the file does not exist on this step.
        // If it is a non-propagatable file, we must return its null value
        // If it is propagatable, they must not appear and disappear,
        // thus we know that there is no such file in the entire history
        return null
      }

      val authorContents = historyStep.authorContents
      if (!historyStep.shouldBePropagated) {
        return authorContents
      }

      // Here we know that the file is propagatable. There are two options about what to return in this case.
      // The first option is used, but the second also makes sense:
      // 1. We return `authorContents` only if the previous step is not propagatable or is absent (i.e., this is the very first step)
      // 2. We don't return anything at all and search further: `currentIndex--`, or return null for the very first step

      if (currentIndex == 0 || remoteHistory[currentIndex - 1]?.shouldBePropagated != true) return authorContents

      currentIndex--
    }

    return null
  }
}

private data class TaskFileStep(
  val authorContents: FileContents,
  val shouldBePropagated: Boolean
)