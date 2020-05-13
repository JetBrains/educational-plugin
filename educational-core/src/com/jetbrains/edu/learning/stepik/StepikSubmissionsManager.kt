package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager

object StepikSubmissionsManager : SubmissionsManager() {

  @JvmStatic
  fun loadMissingSubmissions(course: Course) {
    val newTasks = course.allTasks.filter { !submissions.containsKey(it.id) }
    for (task in newTasks) {
      getAllSubmissions(task.id)
    }
  }

  @JvmStatic
  fun getSubmissions(taskId: Int, isSolved: Boolean): List<Submission> {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getAllSubmissions(taskId).filter { it.status == status }
  }

  @JvmStatic
  fun getLastSubmissionReply(taskId: Int, isSolved: Boolean): Reply? {
    return getLastSubmission(taskId, isSolved)?.reply
  }

  @JvmStatic
  fun addToSubmissions(taskId: Int, submission: Submission?) {
    super.addToSubmissionsMap(taskId, submission)
  }

  @JvmStatic
  fun isLastSubmissionUpToDate(task: Task, isSolved: Boolean): Boolean {
    if (task is TheoryTask) return true
    val submission = getLastSubmission(task.id, isSolved) ?: return false
    return submission.time?.after(task.updateDate) ?: false
  }

  @JvmStatic
  fun prepareStepikSubmissionsContent(project: Project, course: Course) {
    super.prepareSubmissionsContent(project, course)
  }

  @JvmStatic
  fun loadAllStepikSubmissions(project: Project, course: Course) {
    return loadAllSubmissions(project, course)
  }

  private fun getAllSubmissions(stepId: Int): MutableList<Submission> {
    return submissions.getOrPut(stepId) {StepikConnector.getInstance().getAllSubmissions(stepId) }
  }

  override fun loadAllSubmissions(project: Project, course: Course?) {
    if (course is EduCourse && course.isRemote && isLoggedIn()) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val allTasks: List<Task> = course.allTasks
        for (task in allTasks) {
          if(task is ChoiceTask) {
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

  override fun submissionsCanBeShown(course: Course?): Boolean {
    return course is EduCourse && course.isStudy && course.isRemote
  }

  override fun getPlatformName(): String = STEPIK

  override fun isLoggedIn(): Boolean = EduSettings.isLoggedIn()

  override fun addViewOnStepikLink(descriptionText: StringBuilder, currentTask: ChoiceTask, submissionsPanel: AdditionalTabPanel) {
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

  private fun getLastSubmission(taskId: Int, isSolved: Boolean): Submission? {
    val submissions = getSubmissions(taskId, isSolved)
    return submissions.firstOrNull()
  }
}
