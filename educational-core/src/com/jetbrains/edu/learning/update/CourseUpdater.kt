package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.getConfigDir

abstract class CourseUpdater(val project: Project) {
  abstract fun updateCourse(onFinish: (isUpdated: Boolean) -> Unit)

  protected fun updateTaskDescription(task: Task, remoteTask: Task) {
    task.descriptionText = remoteTask.descriptionText
    task.descriptionFormat = remoteTask.descriptionFormat
    task.feedbackLink = remoteTask.feedbackLink
    if (task is ChoiceTask && remoteTask is ChoiceTask) {
      task.choiceOptions = remoteTask.choiceOptions
      task.isMultipleChoice = remoteTask.isMultipleChoice
    }

    // Task Description file needs to be regenerated as it already exists
    GeneratorUtils.createDescriptionFile(project, task.getConfigDir(project), task) ?: return
  }

  protected fun showUpdateCompletedNotification(message: String) {
    Notification("JetBrains Academy", EduCoreBundle.message("update.notification.title"),
                 message,
                 NotificationType.INFORMATION).notify(project)
  }
}