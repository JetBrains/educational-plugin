package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduState.Companion.getEduState
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TopPanel
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

val LOG = Logger.getInstance("HyperskillUtils")

val HYPERSKILL_SELECTED_STAGE: Key<Int> = Key.create("HYPERSKILL_SELECTED_STAGE")
val HYPERSKILL_SELECTED_PROBLEM: Key<Int> = Key.create("HYPERSKILL_SELECTED_PROBLEM")

val failedToPostToJBA: String
  get() = EduCoreBundle.message("error.failed.to.post.solution.with.guide", EduNames.JBA, EduNames.FAILED_TO_POST_TO_JBA_URL)

val hyperskillNotificationGroup: NotificationGroup
  get() = NotificationGroup(EduNames.JBA, NotificationDisplayType.BALLOON, true)

fun openSelectedStage(course: Course, project: Project) {
  if (course !is HyperskillCourse) {
    return
  }
  val stageId = computeSelectedStage(course)
  if (stageId != null) {
    val index = course.stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val lesson = course.lessons[0]
      val taskList = lesson.taskList
      if (taskList.size > index) {
        val fromTask = if (lesson is FrameworkLesson) lesson.currentTask() else taskList[0]
        NavigationUtils.navigateToTask(project, taskList[index], fromTask, false)
      }
    }
  }
}

private fun computeSelectedStage(course: HyperskillCourse): Int? {
  val stageId = course.getUserData(HYPERSKILL_SELECTED_STAGE)
  if (stageId != null) {
    course.putUserData(HYPERSKILL_SELECTED_STAGE, null) // we may want to select something other in the same project
    return stageId
  }
  // do not switch selected stage if a user opened only a single problem
  val stepId = course.getUserData(HYPERSKILL_SELECTED_PROBLEM)
  if (stepId != null) {
    course.putUserData(HYPERSKILL_SELECTED_PROBLEM, null) // we may want to select something other in the same project
    return null
  }
  val projectLesson = course.getProjectLesson() ?: return null
  val firstUnsolvedTask = projectLesson.taskList.indexOfFirst { task -> task.status != CheckStatus.Solved }
  return course.stages[if (firstUnsolvedTask != -1) firstUnsolvedTask else projectLesson.taskList.size - 1].id
}

fun getTopPanelForProblem(project: Project, course: HyperskillCourse, task: Task?): JPanel? {
  if (task == null || course.isTaskInProject(task) || CCUtils.isCourseCreator(project) || course.getProjectLesson() == null) {
    return null
  }
  return TopPanel(EduCoreBundle.message("hyperskill.navigate.to.project", course.presentableName),
                  NavigateToProjectAction(project, course))
}

fun markStageAsCompleted(task: Task) {
  val course = task.course as HyperskillCourse
  val stage = course.stages.getOrNull(task.index - 1) ?: error("No stage for stage ${task.name} in course ${course.name}")
  if (stage.isCompleted) return
  stage.isCompleted = true
  YamlFormatSynchronizer.saveRemoteInfo(course)
}

private class NavigateToProjectAction(private val project: Project, private val course: HyperskillCourse) : DumbAwareAction(null as String?) {
  override fun actionPerformed(e: AnActionEvent) {
    val lesson = course.getProjectLesson() ?: return
    val currentTask = lesson.currentTask() ?: return
    NavigationUtils.navigateToTask(project, currentTask)
  }
}

class HSPeekSolutionAction : CompareWithAnswerAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = getEduState(project)?.task ?: return
    val course = task.course as? HyperskillCourse ?: return
    val url = if (course.isTaskInProject(task)) task.feedbackLink.link else stepLink(task.id)
    if (url.isNullOrEmpty()) return
    EduBrowser.browse("$url#solutions")
  }

  companion object {
    const val ACTION_ID = "Hyperskill.PeekSolution"
  }
}

fun stepLink(stepId: Int) = "${HYPERSKILL_URL}learn/step/$stepId"

fun isHyperskillSupportAvailable(): Boolean = EduConfiguratorManager.allExtensions().any { it.courseType == HYPERSKILL_TYPE }

fun getSelectedProjectIdUnderProgress(account: HyperskillAccount): Int? {
  return computeUnderProgress(null, SYNCHRONIZE_JBA_ACCOUNT, false) {
    val currentUser = HyperskillConnector.getInstance().getCurrentUser(account)
    if (currentUser == null) {
      null
    }
    else {
      account.userInfo = currentUser
      account.userInfo.hyperskillProjectId
    }
  }
}

fun showErrorDetails(project: Project, error: String) {
  if (error == EduCoreBundle.message("error.access.denied")) {
    Notification(
      hyperskillNotificationGroup.displayId,
      EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA),
      EduCoreBundle.message("error.access.denied.with.link"),
      NotificationType.ERROR
    ) { notification, e ->
      notification.expire()
      HyperskillLoginListener.hyperlinkUpdate(e)
    }.notify(project)
    return
  }

  LOG.warn(error)
  Notification(
    hyperskillNotificationGroup.displayId,
    EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA),
    EduCoreBundle.message("help.use.guide", EduNames.FAILED_TO_POST_TO_JBA_URL),
    NotificationType.ERROR,
    NotificationListener.URL_OPENING_LISTENER
  ).notify(project)
}

object HyperskillLoginListener : HyperlinkAdapter() {
  override fun hyperlinkActivated(e: HyperlinkEvent?) {
    HyperskillConnector.getInstance().doAuthorize(Runnable {
      val fullName = HyperskillSettings.INSTANCE.account?.userInfo?.fullname ?: return@Runnable
      Notification(
        hyperskillNotificationGroup.displayId,
        EduCoreBundle.message("login.successful"),
        EduCoreBundle.message("logged.in.as", fullName),
        NotificationType.INFORMATION
      ).notify(null)
    })
  }
}

val HyperskillProject.eduEnvironment: String?
  get() {
    val hyperskillEnvironment = environment
    if (hyperskillEnvironment.isNullOrEmpty()) {
      return EduNames.DEFAULT_ENVIRONMENT
    }
    return HYPERSKILL_ENVIRONMENTS[hyperskillEnvironment]
  }

val Task.successMessage: String
  get() {
    if (!course.isStudy) {
      return CheckUtils.CONGRATULATIONS
    }
    val hyperskillCourse = this.course as HyperskillCourse
    val stageId = hyperskillCourse.stages[this.index - 1].id
    val link = "${HYPERSKILL_PROJECTS_URL}/${hyperskillCourse.hyperskillProject!!.id}/stages/$stageId/implement"
    return "${CheckUtils.CONGRATULATIONS} ${EduCoreBundle.message("hyperskill.continue", link, EduNames.JBA)}"
  }
