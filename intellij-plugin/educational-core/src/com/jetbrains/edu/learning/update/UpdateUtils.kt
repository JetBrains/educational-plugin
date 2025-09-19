package com.jetbrains.edu.learning.update

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.toCourseInfoHolder

object UpdateUtils {

  fun updateTaskDescription(project: Project, task: Task, remoteTask: Task) {
    task.descriptionText = remoteTask.descriptionText
    task.descriptionFormat = remoteTask.descriptionFormat
    task.feedbackLink = remoteTask.feedbackLink

    when (task) {
      is ChoiceTask if remoteTask is ChoiceTask -> {
        task.choiceOptions = remoteTask.choiceOptions
        task.isMultipleChoice = remoteTask.isMultipleChoice
      }
      is SortingTask if remoteTask is SortingTask -> {
        task.options = remoteTask.options
      }
      is MatchingTask if remoteTask is MatchingTask -> {
        task.captions = remoteTask.captions
        task.options = remoteTask.options
      }
      is TableTask if remoteTask is TableTask -> {
        task.createTable(remoteTask.rows, remoteTask.columns, remoteTask.isMultipleChoice)
      }
    }

    // Task Description file needs to be regenerated as it already exists
    val taskDir = task.getTaskDirectory(project)
    if (taskDir != null) {
      GeneratorUtils.createDescriptionFile(project, taskDir, task)
    }
  }

  fun updateFrameworkLessonFiles(project: Project, lesson: FrameworkLesson, task: Task, remoteTask: Task, updatePropagatableFiles: Boolean) {
    fun updateTaskFiles(
      task: Task,
      remoteTaskFiles: Map<String, TaskFile>,
      updateInLocalFS: Boolean
    ) {
      for ((path, remoteTaskFile) in remoteTaskFiles) {
        val taskFile = task.taskFiles[path]
        val currentTaskFile = if (taskFile != null) {
          taskFile.contents = remoteTaskFile.contents
          taskFile
        }
        else {
          task.addTaskFile(remoteTaskFile)
          remoteTaskFile
        }

        if (updateInLocalFS) {
          // remove read only flags, so we can write new content to non-editable files
          // editable flags for task files will be restored in [GeneratorUtils.createChildFile()] call
          removeReadOnlyFlags(project, currentTaskFile)

          val taskDir = task.getDir(project.courseDir)
          if (taskDir != null) {
            GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, path, currentTaskFile.contents, currentTaskFile.isEditable)
          }
        }
      }

      task.init(lesson, false)
    }

    val flm = FrameworkLessonManager.getInstance(project)

    if (lesson.currentTaskIndex != task.index - 1) {
      updateTaskFiles(task, remoteTask.nonPropagatableFiles, false)
      flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.text })
    }
    else {
      if (updatePropagatableFiles && !task.hasChangedFiles(project)) {
        updateTaskFiles(task, remoteTask.taskFiles, true)
      }
      else {
        updateTaskFiles(task, remoteTask.nonPropagatableFiles, true)
      }
    }
  }

  private val Task.nonPropagatableFiles: Map<String, TaskFile>
    get() = taskFiles.filter { !it.value.shouldBePropagated() }

  private fun removeReadOnlyFlags(project: Project, taskFile: TaskFile) {
    val virtualTaskFile = taskFile.getVirtualFile(project) ?: return
    invokeAndWaitIfNeeded {
      runWriteAction {
        GeneratorUtils.removeNonEditableFileFromCourse(taskFile.course(), virtualTaskFile)
      }
    }
  }

  fun FrameworkLesson.shouldFrameworkLessonBeUpdated(lessonFromServer: FrameworkLesson): Boolean {
    val tasksFromServer = lessonFromServer.taskList
    val localTasks = taskList
    return when {
      localTasks.size > tasksFromServer.size -> false
      localTasks.zip(tasksFromServer).any { (task, remoteTask) -> task.id != remoteTask.id } -> false
      else -> true
    }
  }

  fun navigateToTaskAfterUpdate(project: Project) {
    project.invokeLater {
      val currentTask = project.getCurrentTask()
      val course = project.course ?: return@invokeLater

      if (currentTask != null) {
        NavigationUtils.navigateToTask(project, currentTask)
      }
      else {
        NavigationUtils.openFirstTask(course, project)
      }
    }
  }
}