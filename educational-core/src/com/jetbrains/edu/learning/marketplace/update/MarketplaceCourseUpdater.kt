package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseUpdater
import com.jetbrains.edu.learning.EduUtils.getTextFromTaskTextFile
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.findTaskDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector

class MarketplaceCourseUpdater(project: Project, course: EduCourse, val remoteCourseVersion: Int) : EduCourseUpdater(project, course) {
  private val tasksStatuses = mutableMapOf<Int, CheckStatus>()

  override fun doUpdate(courseFromServer: EduCourse) {
    tasksStatuses.clear()

    super.doUpdate(courseFromServer)
    saveLearningProgress(courseFromServer)
  }

  override fun setUpdated(courseFromServer: EduCourse) {
    super.setUpdated(courseFromServer)
    course.marketplaceCourseVersion = remoteCourseVersion
  }

  override fun sectionShouldBeSkipped(sectionId: Int): Boolean = false

  override fun courseFromServer(currentCourse: EduCourse, courseInfo: EduCourse?): EduCourse? {
    val courseFromServer = MarketplaceConnector.getInstance().searchCourse(course.id, currentCourse.isMarketplacePrivate)
    if (courseFromServer != null) {
      MarketplaceConnector.getInstance().loadCourseStructure(courseFromServer)
    }
    return courseFromServer
  }

  override fun taskChanged(newTask: Task, task: Task): Boolean {
    val newTaskFiles = newTask.taskFiles
    val taskFiles = task.taskFiles
    val taskDescriptionText = task.descriptionText.ifEmpty {
      runReadAction {
        getTextFromTaskTextFile(task.findTaskDescriptionFile(project)) ?: ""
      }
    }
    val isChanged = when {
      newTask.name != task.name -> true
      newTask.itemType != task.itemType -> true
      newTask.descriptionText != taskDescriptionText -> true
      newTaskFiles.size != taskFiles.size -> true
      else -> {
        newTaskFiles.any { (newFileName, newTaskFile) ->
          taskFileChanged(newTaskFile, taskFiles[newFileName])
        }
      }
    }
    saveCurrentTaskStatus(task.id, task.status, isChanged)
    return isChanged
  }

  private fun taskFileChanged(newTaskFile: TaskFile, taskFile: TaskFile?): Boolean {
    taskFile ?: return true
    if (newTaskFile.text != taskFile.text) return true
    val newTaskFilePlaceholders = newTaskFile.answerPlaceholders
    val taskFilePlaceholders = taskFile.answerPlaceholders
    if (newTaskFilePlaceholders.size != taskFilePlaceholders.size) return true
    if (newTaskFilePlaceholders.isNotEmpty()) {
      for (i in 0 until newTaskFilePlaceholders.size) {
        val newPlaceholder = newTaskFilePlaceholders[i]
        val placeholder = taskFilePlaceholders[i]
        if (placeholderChanged(newPlaceholder, placeholder)) return true
      }
    }
    return false
  }

  private fun placeholderChanged(newPlaceholder: AnswerPlaceholder, placeholder: AnswerPlaceholder): Boolean =
    newPlaceholder.length != placeholder.initialState.length
    || newPlaceholder.offset != placeholder.initialState.offset
    || newPlaceholder.placeholderText != placeholder.placeholderText
    || newPlaceholder.possibleAnswer != placeholder.possibleAnswer
    || newPlaceholder.placeholderDependency.toString() != placeholder.placeholderDependency.toString()

  // we keep CheckStatus.Solved for task even if it was updated
  // we keep CheckStatus.Failed for task only if it was not updated
  private fun saveCurrentTaskStatus(taskId: Int, checkStatus: CheckStatus, taskChanged: Boolean) {
    if (checkStatus == CheckStatus.Unchecked) return
    if (!taskChanged || checkStatus == CheckStatus.Solved) {
      tasksStatuses[taskId] = checkStatus
    }
  }

  private fun saveLearningProgress(courseFromServer: EduCourse) {
    if (tasksStatuses.isEmpty()) return
    courseFromServer.visitTasks {
      if (it.id in tasksStatuses) {
        val currentStatus = tasksStatuses[it.id]
        if (currentStatus != null) {
          it.status = currentStatus
        }
      }
    }
  }
}
