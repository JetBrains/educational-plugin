package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLoginSuccessfulNotification
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler.getRecommendedAndCompletedSteps
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
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

private class NavigateToProjectAction(private val project: Project, private val course: HyperskillCourse) : DumbAwareAction(
  null as String?) {
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

fun topicCompletedLink(topicId: Int) = "${HYPERSKILL_URL}learn/topic/${topicId}"

fun isHyperskillSupportAvailable(): Boolean = EduConfiguratorManager.allExtensions().any { it.courseType == HYPERSKILL_TYPE }

fun getSelectedProjectIdUnderProgress(account: HyperskillAccount): Int? {
  return computeUnderProgress(null, SYNCHRONIZE_JBA_ACCOUNT, false) {
    val accessToken = account.getAccessToken()
    val currentUser = HyperskillConnector.getInstance().getCurrentUser(account, accessToken)
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
    doLogin()
  }

  fun doLogin() {
    HyperskillConnector.getInstance().doAuthorize(Runnable {
      val fullName = HyperskillSettings.INSTANCE.account?.userInfo?.fullname ?: return@Runnable
      showLoginSuccessfulNotification(fullName)
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

fun notifyJBAUnauthorized(project: Project, specificMessage: String) {
  Notification("EduTools", specificMessage, EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content"),
               NotificationType.ERROR).apply {
    addAction(NotificationAction.createSimpleExpiring(
      EduCoreBundle.message("notification.hyperskill.no.next.activity.login.action")) { HyperskillLoginListener.doLogin() })
  }.notify(project)
}

fun openNextActivity(project: Project, currentTask: Task) {
  if (HyperskillSettings.INSTANCE.account == null) {
    notifyJBAUnauthorized(project, EduCoreBundle.message("notification.hyperskill.no.next.activity.title"))
    return
  }
  val course = currentTask.course
  val language = HYPERSKILL_LANGUAGES.entries.find { it.value == course.language }?.key ?: return

  val nextStepSource = computeUnderProgress(project, EduCoreBundle.message("hyperskill.next.activity"), true) {
    return@computeUnderProgress getNextStepSource(currentTask)
  }

  if (nextStepSource == null) {
    LOG.warn("Next Step is null, currentTaskId: ${currentTask.id}")
    notifyAboutUrlOpening(project, currentTask)
  }
  else {
    val nextStepId = nextStepSource.id
    if (nextStepSource.block?.name in HyperskillCourse.SUPPORTED_STEP_TYPES && nextStepId != currentTask.id) {
      ProjectOpener
        .getInstance()
        .open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepRequest(course.id, nextStepId, language))
    }
    else {
      val topic = nextStepSource.topic
      val link = if (nextStepSource.isCompleted && topic != null) topicCompletedLink(topic) else stepLink(nextStepId)
      LOG.warn(
        "Next step is not supported or already exist in project nextStepId: $nextStepId currentTaskId: ${currentTask.id} topic: $topic")
      EduBrowser.getInstance().browse(link)
    }
  }
}

private fun getNextStepSource(currentTask: Task): HyperskillStepSource? {
  val currentTaskId = currentTask.id
  val topic = HyperskillConnector.getInstance()
    .getStepSource(currentTaskId)
    .onError { return null }.topic

  if (topic != null) {
    val stepsFromServer = getRecommendedAndCompletedSteps(topic, currentTaskId).onError {
      LOG.warn("There is error while getting recommended steps from topic $topic: $it")
      return null
    }
    val stepsInProject = currentTask.lesson.items.map { it.id }.toSet()
    return stepsFromServer.firstOrNull { step -> !stepsInProject.contains(step.id) }
  }
  LOG.warn("Topic is null while getting nextStepSource for currentTaskId: $currentTaskId")
  return null
}

private fun notifyAboutUrlOpening(project: Project, task: Task) {
  Notification(
    "EduTools",
    EduCoreBundle.message("notification.hyperskill.no.next.activity.title"),
    EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(task.id)),
    NotificationType.ERROR,
    NotificationListener.URL_OPENING_LISTENER
  ).notify(project)
}

fun wrapWithUtm(link: String, content: String): String {
  val utmParams = "utm_source=ide&utm_medium=ide&utm_campaign=ide&utm_content=$content"

  // if there are other params, add utms as new ones
  return if (link.contains("?")) {
    "$link&$utmParams"
  }
  else {
    "$link?$utmParams"
  }
}