package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TopPanel
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

private val LOG: Logger = Logger.getInstance("HyperskillUtils")

val HYPERSKILL_SELECTED_STAGE: Key<Int> = Key.create("HYPERSKILL_SELECTED_STAGE")
val HYPERSKILL_SELECTED_PROBLEM: Key<Int> = Key.create("HYPERSKILL_SELECTED_PROBLEM")

val failedToPostToJBA: String
  get() = EduCoreBundle.message("error.failed.to.post.solution.with.guide", EduNames.JBA, EduNames.FAILED_TO_POST_TO_JBA_URL)

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
  val stageId = course.dataHolder.getUserData(HYPERSKILL_SELECTED_STAGE)
  if (stageId != null) {
    course.dataHolder.putUserData(HYPERSKILL_SELECTED_STAGE, null) // we may want to select something other in the same project
    return stageId
  }
  // do not switch selected stage if a user opened only a single problem
  val stepId = course.dataHolder.getUserData(HYPERSKILL_SELECTED_PROBLEM)
  if (stepId != null) {
    course.dataHolder.putUserData(HYPERSKILL_SELECTED_PROBLEM, null) // we may want to select something other in the same project
    return null
  }
  val projectLesson = course.getProjectLesson() ?: return null
  val firstUnsolvedTask = projectLesson.taskList.indexOfFirst { task -> task.status != CheckStatus.Solved }
  if (firstUnsolvedTask == -1 && projectLesson.taskList.size < 1) return null
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

fun hyperskillTaskLink(task: Task): String {
  val course = task.course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
  return if (course.isTaskInProject(task)) stageLink(task) else stepLink(task.id)
}

fun stageLink(task: Task): String {
  val course = task.course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
  val projectId = course.hyperskillProject?.id ?: error("Course doesn't have Hyperskill project")
  val stageId = course.stages[task.index - 1].id
  return stageLink(projectId, stageId)
}

fun stageLink(projectId: Int, stageId: Int) = "$HYPERSKILL_PROJECTS_URL/$projectId/stages/$stageId/implement"

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
      "EduTools",
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
    "EduTools",
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
        "EduTools",
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

val HyperskillAccount.profileUrl: String get() = "$HYPERSKILL_PROFILE_PATH${userInfo.id}"

fun markTheoryTaskAsCompleted(project: Project, task: TheoryTask) {
  runInBackground(project, EduCoreBundle.message("hyperskill.posting.theory"), false) {
    HyperskillConnector.getInstance().markTheoryCompleted(task.id)
  }
}

fun Task.getRelatedTheoryTask(): TheoryTask? {
  course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
  if (this is TheoryTask) {
    error("Function is called for Theory task itself")
  }
  return lesson.taskList.find { it is TheoryTask } as? TheoryTask
}