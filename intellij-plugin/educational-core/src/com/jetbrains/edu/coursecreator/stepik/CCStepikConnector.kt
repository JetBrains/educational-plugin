package com.jetbrains.edu.coursecreator.stepik

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.collectAdditionalLessonInfo
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorizedToStepik
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.updateOnStepikTitleMessage
import com.jetbrains.edu.coursecreator.uploadToStepikTitleMessage
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import org.apache.http.HttpStatus
import org.jetbrains.annotations.NonNls
import java.util.stream.Collectors

object CCStepikConnector {
  private val LOG = Logger.getInstance(CCStepikConnector::class.java.name)

  // POST methods:
  fun postLesson(project: Project, lesson: Lesson, position: Int, sectionId: Int): Boolean {
    val postedLesson = postLessonInfo(project, lesson, sectionId, position) ?: return false
    postedLesson.index = lesson.index
    postedLesson.items = lesson.items
    postedLesson.parent = lesson.parent
    var success = true
    for (task in lesson.taskList) {
      checkCanceled()
      success = postTask(project, task, postedLesson.id) && success
    }
    if (!updateLessonAdditionalInfo(lesson, project)) {
      showFailedToPostItemNotification(project, lesson, true)
      return false
    }
    val parent = lesson.parent
    parent.removeItem(lesson)
    parent.addItem(lesson.index - 1, postedLesson)
    return success
  }

  private fun postLessonInfo(project: Project, lesson: Lesson, sectionId: Int, position: Int): Lesson? {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.LESSON_TYPE.uploadToStepikTitleMessage)) return null

    val postedLesson = StepikConnector.getInstance().postLesson(lesson)
    if (postedLesson == null) {
      showFailedToPostItemNotification(project, lesson, true)
      return null
    }
    if (sectionId != -1) {
      postedLesson.unitId = postUnit(postedLesson.id, position, sectionId, project)
    }
    // required to POST additional files and form lesson creation notification link
    lesson.id = postedLesson.id
    return postedLesson
  }

  private fun postUnit(lessonId: Int, position: Int, sectionId: Int, project: Project): Int {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.LESSON_TYPE.uploadToStepikTitleMessage)) return lessonId
    val unit = StepikConnector.getInstance().postUnit(lessonId, position, sectionId)
    if (unit?.id == null) {
      showErrorNotification(project, message("course.creator.stepik.failed.to.post.unit"))
      return -1
    }
    return unit.id ?: return -1
  }

  private fun postTask(project: Project, task: Task, lessonId: Int): Boolean {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.TASK_TYPE.uploadToStepikTitleMessage)) return false
    if (task is CodeTask) return true
    val stepSource = StepikConnector.getInstance().postTask(project, task, lessonId)
    if (stepSource == null) {
      showFailedToPostItemNotification(project, task, true)
      return false
    }
    task.id = stepSource.id
    task.updateDate = stepSource.updateDate
    return true
  }

  // UPDATE methods:
  fun updateLesson(
    project: Project,
    lesson: Lesson,
    showNotification: Boolean,
    sectionId: Int
  ): Boolean {
    val postedLesson = updateLessonInfo(project, lesson, showNotification, sectionId)
    return postedLesson != null &&
           updateLessonTasks(project, lesson, postedLesson.stepIds) &&
           updateLessonAdditionalInfo(lesson, project)
  }

  private fun updateLessonInfo(
    project: Project,
    lesson: Lesson,
    showNotification: Boolean, sectionId: Int
  ): StepikLesson? {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.LESSON_TYPE.updateOnStepikTitleMessage)) return null
    val updatedLesson = StepikConnector.getInstance().updateLesson(lesson)
    if (updatedLesson == null) {
      if (showNotification) {
        showFailedToPostItemNotification(project, lesson, false)
      }
      return null
    }
    if (sectionId != -1) {
      updateUnit(updatedLesson.unitId, lesson.id, lesson.index, sectionId, project)
    }
    return updatedLesson
  }

  private fun updateLessonAdditionalInfo(lesson: Lesson, project: Project): Boolean {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.LESSON_TYPE.updateOnStepikTitleMessage)) return false
    val info = collectAdditionalLessonInfo(lesson, project)
    if (info.isEmpty) {
      StepikConnector.getInstance().deleteLessonAttachment(lesson.id)
      return true
    }
    updateProgress(message("course.creator.stepik.progress.details.publishing.additional.data", lesson.presentableName))
    return StepikConnector.getInstance().updateLessonAttachment(info, lesson) == HttpStatus.SC_CREATED
  }

  private fun updateUnit(unitId: Int, lessonId: Int, position: Int, sectionId: Int, project: Project) {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.LESSON_TYPE.updateOnStepikTitleMessage)) return
    val unit = StepikConnector.getInstance().updateUnit(unitId, lessonId, position, sectionId)
    if (unit == null) {
      showErrorNotification(project, message("course.creator.stepik.failed.to.update.unit"))
    }
  }

  private fun updateLessonTasks(project: Project, localLesson: Lesson, steps: List<Int>): Boolean {
    val localTasksIds = localLesson.taskList
      .stream()
      .map { task: Task -> task.id }
      .filter { id: Int -> id > 0 }
      .collect(Collectors.toSet())
    val taskIdsToDelete = steps.stream()
      .filter { id: Int -> !localTasksIds.contains(id) }
      .collect(Collectors.toList())

    // Remove all tasks from Stepik which are not in our lessons now
    for (step in taskIdsToDelete) {
      StepikConnector.getInstance().deleteTask(step)
    }
    var success = true
    for (task in localLesson.taskList) {
      checkCanceled()
      success = (if (task.id > 0) updateTask(project, task) else postTask(project, task, localLesson.id)) && success
    }
    return success
  }

  private fun updateTask(project: Project, task: Task): Boolean {
    if (!checkIfAuthorizedToStepik(project, StudyItemType.TASK_TYPE.updateOnStepikTitleMessage)) return false
    task.getDir(project.courseDir) ?: return false
    return when (StepikConnector.getInstance().updateTask(project, task)) {
      HttpStatus.SC_OK -> {
        val step = StepikConnector.getInstance().getStep(task.id)
        if (step != null) {
          task.updateDate = step.updateDate
        }
        else {
          LOG.warn("Failed to get step for task '${task.name}' with id ${task.id} while setting an update date")
        }
        true
      }

      HttpStatus.SC_NOT_FOUND ->
        postTask(project, task, task.lesson.id)

      HttpStatus.SC_FORBIDDEN -> {
        showNoRightsToUpdateOnStepikNotification(project)
        false
      }

      else -> {
        showFailedToPostItemNotification(project, task, false)
        false
      }
    }
  }

  private fun showNoRightsToUpdateOnStepikNotification(project: Project) {
    showErrorNotification(
      project,
      message("notification.course.creator.access.denied.title"),
      message("notification.course.creator.access.denied.content")
    )
  }

  private fun showFailedToPostItemNotification(project: Project, item: StudyItem, isNew: Boolean) {
    val title = if (isNew) {
      message("notification.course.creator.failed.to.upload.item.title")
    }
    else {
      message("notification.course.creator.failed.to.update.item.title")
    }

    val pathInCourse = item.pathInCourse
    val content = if (isNew) {
      message("notification.course.creator.failed.to.upload.item.content", pathInCourse)
    }
    else {
      message("notification.course.creator.failed.to.update.item.content", pathInCourse)
    }

    showErrorNotification(project, title, content, showLogAction)
  }

  // helper methods:
  private fun updateProgress(text: @NlsContexts.ProgressDetails String) {
    val indicator = ProgressManager.getInstance().progressIndicator ?: return
    indicator.checkCanceled()
    indicator.text2 = text
  }

  fun openOnStepikAction(url: @NonNls String): AnAction {
    return object : AnAction(message("action.open.on.text", STEPIK)) {
      override fun actionPerformed(e: AnActionEvent) {
        EduBrowser.getInstance().browse(getStepikUrl() + url)
      }
    }
  }

  private fun checkCanceled() {
    val indicator = ProgressManager.getInstance().progressIndicator
    indicator?.checkCanceled()
  }
}
