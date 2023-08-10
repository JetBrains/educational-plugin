package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkAdapter
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLoginSuccessfulNotification
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAccount
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import javax.swing.event.HyperlinkEvent

private val LOG: Logger = Logger.getInstance("HyperskillUtils")

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
  val stageId = course.selectedStage
  if (stageId != null) {
    course.selectedStage = null  // we may want to select something other in the same project
    return stageId
  }
  // do not switch selected stage if a user opened only a single problem
  val problemId = course.selectedProblem
  if (problemId != null) {
    course.selectedProblem = null // we may want to select something other in the same project
    return null
  }
  val projectLesson = course.getProjectLesson() ?: return null
  val firstUnsolvedTask = projectLesson.taskList.indexOfFirst { task -> task.status != CheckStatus.Solved }
  if (firstUnsolvedTask == -1 && projectLesson.taskList.isEmpty()) return null
  return course.stages[if (firstUnsolvedTask != -1) firstUnsolvedTask else projectLesson.taskList.size - 1].id
}

fun markStageAsCompleted(task: Task) {
  val course = task.course as HyperskillCourse
  val stage = course.stages.getOrNull(task.index - 1) ?: error("No stage for stage ${task.name} in course ${course.name}")
  if (stage.isCompleted) return
  stage.isCompleted = true
  YamlFormatSynchronizer.saveRemoteInfo(course)
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

/**
 * If lesson is not complete when next step appear
 */
fun topicCompletedLink(topicId: Int) = "${HYPERSKILL_URL}learn/topic/${topicId}"

fun isHyperskillSupportAvailable(): Boolean = EduConfiguratorManager.allExtensions().any { it.courseType == HYPERSKILL }

fun getSelectedProjectIdUnderProgress(): Int? {
  return computeUnderProgress(null, EduCoreBundle.message("hyperskill.synchronizing.account"), false) {
    val currentUser = HyperskillConnector.getInstance().getCurrentUserInfo() ?: return@computeUnderProgress null
    currentUser.hyperskillProjectId
  }
}

object HyperskillLoginListener : HyperlinkAdapter() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    doLogin()
  }

  fun doLogin() {
    HyperskillConnector.getInstance().doAuthorize(Runnable {
      val fullName = HyperskillSettings.INSTANCE.account?.userInfo?.getFullName() ?: return@Runnable
      showLoginSuccessfulNotification(fullName)
    })
  }
}

val HyperskillProject.eduEnvironment: String?
  get() {
    val hyperskillEnvironment = environment
    if (hyperskillEnvironment.isNullOrEmpty()) {
      return DEFAULT_ENVIRONMENT
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
  if (course !is HyperskillCourse) return null
  if (this is TheoryTask) {
    LOG.warn("Function is called for Theory task itself")
    return null
  }
  return lesson.taskList.find { it is TheoryTask } as? TheoryTask
}

fun notifyJBAUnauthorized(project: Project, specificMessage: String) {
  Notification("JetBrains Academy", specificMessage, EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content"),
               NotificationType.ERROR).apply {
    addAction(NotificationAction.createSimpleExpiring(
      EduCoreBundle.message("notification.hyperskill.no.next.activity.login.action")) { HyperskillLoginListener.doLogin() })
  }.notify(project)
}

fun openNextActivity(project: Project, task: Task) {
  if (HyperskillSettings.INSTANCE.account == null) {
    notifyJBAUnauthorized(project, EduCoreBundle.message("notification.hyperskill.no.next.activity.title"))
    return
  }

  val nextActivityInfo = computeUnderProgress(project, EduCoreBundle.message("hyperskill.next.activity"), true) {
    getNextStep(task.id)
  }

  when (nextActivityInfo) {
    is NextActivityInfo.TopicCompleted -> EduBrowser.getInstance().browse(topicCompletedLink(nextActivityInfo.topicId))
    NextActivityInfo.NoTopic, NextActivityInfo.NoActivity -> showNoNextActivityNotification(task, project)
    is NextActivityInfo.NotCalculated -> {
      showNoNextActivityNotification(task, project)
      LOG.warn("Next step for taskId=${task.id}, topicId=${nextActivityInfo.topicId} isn't calculated yet: current step with not " +
               "completed state is returned")
    }
    is NextActivityInfo.Next -> {
      val nextStep = nextActivityInfo.stepSource
      val topicId = nextActivityInfo.topicId
      if (!HyperskillCourse.isStepSupported(nextStep.block?.name)) {
        EduBrowser.getInstance().browse(stepLink(nextStep.id))
        LOG.warn("Step is not supported: next stepId ${nextStep.id}, current task: ${task.id} topic: ${topicId} ")
      }

      val course = task.course
      val language = HyperskillLanguages.getRequestLanguage(course.languageId) ?: return
      ProjectOpener.getInstance().open(
        HyperskillOpenInIdeRequestHandler,
        HyperskillOpenStepWithProjectRequest(course.id, nextStep.id, language)
      ).onError {
        logger<ProjectOpener>().warn("Opening the next activity resulted in an error: ${it.message}. The error was ignored and not displayed for the user.")
      }
    }
  }
}

private fun getTopic(taskId: Int): Int? {
  val stepSource = HyperskillConnector.getInstance().getStepSource(taskId).onError {
    LOG.warn(it)
    null
  }

  return stepSource?.topic
}

private fun getNextStep(taskId: Int): NextActivityInfo {
  val topicId = getTopic(taskId)
  if (topicId == null) {
    LOG.warn("Topic id is null for a step ${taskId}")
    return NextActivityInfo.NoTopic
  }

  val steps = HyperskillConnector.getInstance().getStepsForTopic(topicId).onError { error ->
    LOG.warn(error)
    null
  }

  if (steps.isNullOrEmpty()) {
    return NextActivityInfo.NoActivity
  }

  // it can happen as next task is calculated asynchronously,
  // But as it never happens during testing I'll log it to see if we need to process this case
  val currentStep = steps.find { it.id == taskId }
  if (currentStep?.isCompleted == false) {
    return NextActivityInfo.NotCalculated(topicId)
  }

  val nextStep = steps.find { it.isNext }

  return if (nextStep == null) {
    NextActivityInfo.TopicCompleted(topicId)
  }
  else {
    NextActivityInfo.Next(nextStep, topicId)
  }
}

private fun showNoNextActivityNotification(task: Task, project: Project) {
  Notification(
    "JetBrains Academy",
    EduCoreBundle.message("notification.hyperskill.no.next.activity.title"),
    EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(task.id)),
    NotificationType.ERROR
  )
    .setListener(NotificationListener.URL_OPENING_LISTENER)
    .notify(project)
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

private sealed class NextActivityInfo {
  class TopicCompleted(val topicId: Int) : NextActivityInfo()

  object NoActivity : NextActivityInfo()

  object NoTopic : NextActivityInfo()

  class Next(val stepSource: HyperskillStepSource, val topicId: Int) : NextActivityInfo()

  class NotCalculated(val topicId: Int) : NextActivityInfo()
}
