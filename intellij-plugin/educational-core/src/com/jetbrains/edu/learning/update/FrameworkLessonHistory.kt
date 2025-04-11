package com.jetbrains.edu.learning.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.ext.shouldBePropagated
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.FrameworkLessonManager

class FrameworkLessonHistory private constructor(val taskFileHistories: Map<String, FrameworkLessonTaskFileHistory>) {

  companion object {
    fun create(
      project: Project,
      localLesson: FrameworkLesson,
      remoteLesson: FrameworkLesson,
      propagateFilesOnNavigation: Boolean
    ): FrameworkLessonHistory {
      // collect a set of all task file names from both local and remote lessons
      val localFileNames = localLesson.taskList.flatMap { it.taskFiles.keys }
      val remoteFileNames = remoteLesson.taskList.flatMap { it.taskFiles.keys }
      val allTaskFiles = localFileNames.toSet() + remoteFileNames.toSet()

      val taskFileHistories = allTaskFiles.associateWith { fileName ->
        FrameworkLessonTaskFileHistory.create(
          project,
          localLesson,
          remoteLesson,
          fileName,
          propagateFilesOnNavigation
        )
      }

      return FrameworkLessonHistory(taskFileHistories)
    }
  }
}

class FrameworkLessonTaskFileHistory private constructor(private val remoteHistory: List<TaskFileStep>) {

  fun evaluateContents(index: Int): FileContents? {
    val step = remoteHistory.getOrNull(index) ?: return null
    return step.actualContents?.let { InMemoryTextualContents(it) }
  }

  companion object {

    fun create(
      project: Project,
      localLesson: FrameworkLesson,
      remoteLesson: FrameworkLesson,
      fileName: String,
      propagateFilesOnNavigation: Boolean
    ): FrameworkLessonTaskFileHistory {
      // In the local history we detect user changes by comparing actual files with the unmodified file contents
      val localHistory = evaluateHistory(localLesson, fileName, propagateFilesOnNavigation) { index, task, unmodifiedText ->
        val flManager = FrameworkLessonManager.getInstance(project)
        val actualText = flManager.getTaskState(localLesson, task)[fileName]

        if (actualText == unmodifiedText) { null } else { actualText }
      }

      // In the remote history we get user changes from the localHistory evaluated on the previous step
      val remoteHistory = evaluateHistory(remoteLesson, fileName, propagateFilesOnNavigation) { index, task, unmodifiedText ->
        localHistory.getOrNull(index)?.userModification
      }

      return FrameworkLessonTaskFileHistory(remoteHistory)
    }

    private fun evaluateHistory(
      lesson: FrameworkLesson,
      fileName: String,
      propagateFilesOnNavigation: Boolean,
      getUserChanges: (Int, Task, String?) -> String?
    ): List<TaskFileStep> {
      val changesHistory = mutableListOf<TaskFileStep>()

      // iterate the lesson steps, storing the text of the previous step
      var previousText: String? = null
      var previousIsPropagatable: Boolean? = null

      for ((index, task) in lesson.taskList.withIndex()) {
        val taskFile = task.getTaskFile(fileName)

        // The initial text with which the step starts.
        // First, the user sees it and then either modifies or not
        // It could be a file.text for non-propagatable files, or the text propagated from the previous step.
        val unmodifiedText = when {
          !propagateFilesOnNavigation -> taskFile?.text
          previousIsPropagatable == true -> previousText
          else -> taskFile?.text
        }

        val userChanges = getUserChanges(index, task, unmodifiedText)
        val taskFileStep = TaskFileStep(unmodifiedText, userChanges)
        changesHistory.add(taskFileStep)

        previousText = taskFileStep.actualContents
        previousIsPropagatable = taskFile?.shouldBePropagated()
        if (previousIsPropagatable == null && taskFileStep.actualContents != null) {
          // taskFile == null, but non-null contents means that the file is created by a user. Let's propagate it
          previousIsPropagatable = true
        }
      }

      return changesHistory
    }
  }
}

private data class TaskFileStep(
  /**
   * Contents, if there are no modifications on this step
   */
  val unmodifiedContents: String?,
  /**
   * Contents by user, or `null` if there are no user changes
   */
  val userModification: String?
) {
  /**
   * The contents visible at this step
   */
  val actualContents: String? get() = userModification ?: unmodifiedContents
}