package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.hasChangedFiles
import com.jetbrains.edu.learning.courseFormat.ext.shouldBePropagated
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.getConfigDir

object UpdateUtils {

  fun updateTaskDescription(project: Project, task: Task, remoteTask: Task) {
    task.descriptionText = remoteTask.descriptionText
    task.descriptionFormat = remoteTask.descriptionFormat
    task.feedbackLink = remoteTask.feedbackLink

    when {
      task is ChoiceTask && remoteTask is ChoiceTask -> {
        task.choiceOptions = remoteTask.choiceOptions
        task.isMultipleChoice = remoteTask.isMultipleChoice
      }
      task is SortingTask && remoteTask is SortingTask -> {
        task.options = remoteTask.options
      }
      task is MatchingTask && remoteTask is MatchingTask -> {
        task.captions = remoteTask.captions
        task.options = remoteTask.options
      }
      task is TableTask && remoteTask is TableTask -> {
        task.createTable(remoteTask.rows, remoteTask.columns, remoteTask.isMultipleChoice)
      }
    }

    // Task Description file needs to be regenerated as it already exists
    GeneratorUtils.createDescriptionFile(project, task.getConfigDir(project), task) ?: return
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
          taskFile.apply {
            text = remoteTaskFile.text
            isEditable = remoteTaskFile.isEditable
          }
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
            GeneratorUtils.createChildFile(project, taskDir, path, currentTaskFile.text, currentTaskFile.isEditable)
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
    runWriteActionAndWait {
      val virtualTaskFile = taskFile.getVirtualFile(project) ?: return@runWriteActionAndWait
      GeneratorUtils.removeNonEditableFileFromCourse(taskFile.course(), virtualTaskFile)
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

  fun showUpdateCompletedNotification(project: Project, message: String) {
    Notification("JetBrains Academy", EduCoreBundle.message("update.notification.title"),
                 message,
                 NotificationType.INFORMATION).notify(project)
  }
}