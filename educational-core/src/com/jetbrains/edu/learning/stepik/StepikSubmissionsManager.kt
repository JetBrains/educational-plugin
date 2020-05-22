package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager

class StepikSubmissionsManager : SubmissionsManager() {

  override fun getAllSubmissions(stepId: Int): MutableList<Submission> {
    return submissions.getOrPut(stepId) {StepikConnector.getInstance().getAllSubmissions(stepId, this) }
  }

  override fun loadAllSubmissions(project: Project, course: Course?) {
    if (course is EduCourse && course.isRemote && isLoggedIn()) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val allTasks: List<Task> = course.allTasks
        for (task in allTasks) {
          if (task is ChoiceTask) {
            putToSubmissions(task.id, mutableListOf())
          }
          else if (task is CodeTask || task is EduTask) {
            getAllSubmissions(task.id)
          }
        }
        ApplicationManager.getApplication().invokeLater {
          TaskDescriptionView.getInstance(project).updateSubmissionsTab()
        }
      }
    }
  }

  override fun getAllSubmissions(stepIds: Set<Int>): List<Submission>? {
    val loadedSubmissions = mutableListOf<Submission>()
    val newStepIds = stepIds.filter { !submissions.containsKey(it) }
    for (stepId in newStepIds) {
      loadedSubmissions.addAll(getAllSubmissions(stepId))
    }
    return loadedSubmissions
  }

  override fun submissionsCanBeShown(course: Course?): Boolean {
    return course is EduCourse && course.isStudy && course.isRemote
  }

  override fun getPlatformName(): String = STEPIK

  override fun isLoggedIn(): Boolean = EduSettings.isLoggedIn()

  override fun addViewOnPlatformLink(descriptionText: StringBuilder, currentTask: ChoiceTask, submissionsPanel: AdditionalTabPanel) {
    descriptionText.append(
      "<a ${StyleManager().textStyleHeader};color:${ColorUtil.toHex(hyperlinkColor())} " +
      "href=https://stepik.org/submissions/${currentTask.id}?unit=${currentTask.lesson.unitId}\">" +
      EduCoreBundle.message("submissions.view.quiz.on.stepik", "</a><a ${StyleManager().textStyleHeader}>"))
    submissionsPanel.addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)
  }

  override fun doAuthorize() {
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(STEPIK, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}
