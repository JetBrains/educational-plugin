package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.getChangeNotesVirtualFile
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.learning.EduCourseUpdater
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.courseConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.DownloadCourseContext.UPDATE
import com.jetbrains.edu.learning.update.UpdateUtils
import kotlinx.coroutines.runBlocking

class MarketplaceCourseUpdater(project: Project, course: EduCourse, private val remoteCourseVersion: Int) : EduCourseUpdater(project, course) {
  private val tasksStatuses = mutableMapOf<Int, CheckStatus>()

  override fun doUpdate(courseFromServer: EduCourse) {
    if (isFeatureEnabled(EduExperimentalFeatures.NEW_COURSE_UPDATE)) {
      runBlocking {
        MarketplaceCourseUpdaterNew(project, course).update(courseFromServer)
      }
      doAfterUpdate()
      return
    }

    tasksStatuses.clear()

    courseFromServer.items.withIndex().forEach { (index, item) -> item.index = index + 1 }

    setCourseInfo(courseFromServer)
    updateSections(courseFromServer)
    updateLessons(courseFromServer)
    updateAdditionalMaterialsFiles(courseFromServer)
    setCourseItems(courseFromServer.items)
    setUpdated(courseFromServer)

    saveLearningProgress(courseFromServer)

    doAfterUpdate()
  }

  private fun doAfterUpdate() {
    UpdateUtils.navigateToTaskAfterUpdate(project)
    //remove editor notification, suggesting to update course
    EditorNotifications.getInstance(project).updateAllNotifications()
    showUpdateNotification()
  }

  private fun showUpdateNotification() {
    val changeNotesVirtualFile = getChangeNotesVirtualFile(project)
    val openChangeNotesAction = if (changeNotesVirtualFile == null || changeNotesVirtualFile.length <= 0) {
      null
    }
    else {
      object : AnAction(EduCoreBundle.message("marketplace.see.whats.new")) {
        override fun actionPerformed(e: AnActionEvent) {
          runInEdt { FileEditorManager.getInstance(project).openFile(changeNotesVirtualFile, true) }
        }
      }
    }

    CCNotificationUtils.showInfoNotification(project, EduCoreBundle.message("action.course.updated"), action = openChangeNotesAction)
  }

  private fun setUpdated(courseFromServer: EduCourse) {
    course.updateDate = courseFromServer.updateDate
    course.marketplaceCourseVersion = remoteCourseVersion
  }

  override fun courseFromServer(currentCourse: EduCourse): EduCourse? {
    val courseFromServer = course.courseConnector.searchCourse(course.id, currentCourse.isMarketplacePrivate)
    if (courseFromServer != null) {
      course.courseConnector.loadCourseStructure(courseFromServer, UPDATE)
    }
    return courseFromServer
  }

  override fun taskChanged(newTask: Task, task: Task): Boolean {
    val newTaskFiles = newTask.taskFiles
    val taskFiles = task.taskFiles
    val taskDescriptionText = runReadAction { task.getDescriptionFile(project, guessFormat = true)?.getTextFromTaskTextFile() ?: "" }

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
