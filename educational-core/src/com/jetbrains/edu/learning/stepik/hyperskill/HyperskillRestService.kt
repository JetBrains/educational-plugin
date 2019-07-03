package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConnector.getTasks
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.ide.RestService
import org.jetbrains.io.send
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

class HyperskillRestService : OAuthRestService(HYPERSKILL) {
  override fun getServiceName(): String = EDU_HYPERSKILL_SERVICE_NAME

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val openCourseMatcher = OPEN_COURSE_PATTERN.matcher(uri)
    val openStepMatcher = OPEN_STEP_PATTERN.matcher(uri)
    val pluginInfo = PLUGIN_INFO.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || openCourseMatcher.matches() || openStepMatcher.matches() ||
                                                       pluginInfo.matches())) {
      true
    }
    else super.isHostTrusted(request)
  }

  @Throws(IOException::class)
  override fun execute(urlDecoder: QueryStringDecoder,
                       request: FullHttpRequest,
                       context: ChannelHandlerContext): String? {
    val uri = urlDecoder.uri()
    if (PLUGIN_INFO.matcher(uri).matches()) {
      createResponse(ObjectMapper().writeValueAsString(PluginInfo(getIdeVersion(), pluginVersion(EduNames.PLUGIN_ID))))
        .send(context.channel(), request)
      return null
    }
    val matcher = OPEN_COURSE_PATTERN.matcher(uri)
    if (matcher.matches()) {
      val account = HyperskillSettings.INSTANCE.account
      if (account == null) {
        HyperskillConnector.doAuthorize(Runnable { openProject(urlDecoder, request, context) })
      }
      else {
        return openProject(urlDecoder, request, context)
      }
    }

    val openStepMatcher = OPEN_STEP_PATTERN.matcher(uri)
    if (openStepMatcher.matches()) {
      return openStep(urlDecoder, request, context)
    }

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = getStringParameter("code", urlDecoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.login(code)
      if (success) {
        RestService.LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = getInternalTemplateText("hyperskill.redirectPage.html")
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  private fun openStep(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stepId = getIntParameter("step_id", urlDecoder)
    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: error("Cannot find project")
    val course = StudyTaskManager.getInstance(project).course
    if (course !is HyperskillCourse) {
      return sendErrorResponse(request, context, "Could not find opened hyperskill project")
    }
    val lesson = findOrCreateProblemsLesson(course, project)
    val lessonDir = lesson.getLessonDir(project)
                    ?: return sendErrorResponse(request, context, "Could not find Problems directory")

    val stepSource = ProgressManager.getInstance().run(
      object : Task.WithResult<HyperskillStepSource?, Exception>(null, "Loading hyperskill problem", true) {
        override fun compute(indicator: ProgressIndicator): HyperskillStepSource? {
          return HyperskillConnector.getStepSource(stepId)
        }
      }) ?: return sendErrorResponse(request, context, "Could not find get step source for the task")

    val task = findOrCreateTask(course, lesson, stepSource, lessonDir, project)

    runInEdt {
      NavigationUtils.navigateToTask(project, task)
      requestFocus()
    }
    sendOk(request, context)
    return null
  }

  private fun findOrCreateTask(course: HyperskillCourse, lesson: Lesson, stepSource: HyperskillStepSource,
                               lessonDir: VirtualFile, project: Project): com.jetbrains.edu.learning.courseFormat.tasks.Task {
    var task = lesson.getTask(stepSource.id)
    if (task == null) {
      task = getTasks(course, lesson, listOf(stepSource)).first()
      task.name = stepSource.title
      task.feedbackLink = FeedbackLink(stepLink(task.id))
      task.index = lesson.taskList.size + 1
      task.descriptionText = "<b>${task.name}</b> ${openOnHyperskillLink(task.id)}" +
                             "<br/><br/>${task.descriptionText}" +
                             "<br/>${openTheoryLink(stepSource.topicTheory)}"
      lesson.addTask(task)
      task.init(course, lesson, false)

      GeneratorUtils.createTask(task, lessonDir)

      course.configurator?.courseBuilder?.refreshProject(project)
    }
    return task
  }

  private fun openTheoryLink(stepId: Int?) =
    if (stepId != null) "<a href=\"${stepLink(stepId)}\">Show topic summary</a>" else ""

  private fun openOnHyperskillLink(stepId: Int) = "<a class=\"right\" href=\"${stepLink(stepId)}\">Open on Hyperskill</a>"

  private fun findOrCreateProblemsLesson(course: HyperskillCourse, project: Project): Lesson {
    var lesson = course.getLesson(HYPERSKILL_PROBLEMS)
    if (lesson == null) {
      lesson = Lesson()
      lesson.name = HYPERSKILL_PROBLEMS
      lesson.index = 2
      course.addLesson(lesson)
      lesson.init(course, null, false)
      GeneratorUtils.createLesson(lesson, course.getDir(project))
    }
    return lesson
  }

  private fun openProject(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getStringParameter("stage_id", decoder)?.toInt() ?: return "The stage_id parameter was not found"
    val projectId = getStringParameter("project_id", decoder)?.toInt() ?: return "The project_id parameter was not found"
    LOG.info("Opening a stage $stageId from project $projectId")

    if (focusOpenProject(projectId, stageId) || openRecentProject(projectId, stageId) || createProject(projectId, stageId)) {
      sendOk(request, context)
      LOG.info("Hyperskill project opened: $projectId")
      return null
    }
    sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
    val message = "A project wasn't found or created"
    LOG.info(message)
    return message
  }

  private fun focusOpenProject(courseId: Int, stageId: Int): Boolean {
    val (project, course) = EduBuiltInServerUtils.focusOpenProject { it is HyperskillCourse && it.hyperskillProject.id == courseId }
                            ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    runInEdt { openSelectedStage(course, project) }
    return true
  }

  private fun openRecentProject(courseId: Int, stageId: Int): Boolean {
    val (_, course) = EduBuiltInServerUtils.openRecentProject { it is HyperskillCourse && it.hyperskillProject.id == courseId }
                      ?: return false
    course?.putUserData(HYPERSKILL_STAGE, stageId)
    return true
  }

  private fun createProject(projectId: Int, stageId: Int): Boolean {
    runInEdt {
      requestFocus()

      val hyperskillCourse = ProgressManager.getInstance().run(object : Task.WithResult<HyperskillCourse?, Exception>
                                                                        (null, "Loading project", true) {
        override fun compute(indicator: ProgressIndicator): HyperskillCourse? {
          val hyperskillProject = HyperskillConnector.getProject(projectId) ?: return null

          if (!hyperskillProject.useIde) {
            LOG.warn("Project in not supported yet $projectId")
            Notification(HYPERSKILL, HYPERSKILL, HYPERSKILL_PROJECT_NOT_SUPPORTED, NotificationType.WARNING,
                         HSHyperlinkListener(false)).notify(project)
            return null
          }
          val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language]
          if (languageId == null) {
            LOG.warn("Language in not supported yet ${hyperskillProject.language}")
            Notification(HYPERSKILL, HYPERSKILL, "Unsupported language ${hyperskillProject.language}",
                         NotificationType.WARNING).notify(project)
            return null
          }
          val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
          val stages = HyperskillConnector.getStages(projectId) ?: return null
          hyperskillCourse.stages = stages
          return hyperskillCourse
        }
      }) ?: return@runInEdt

      hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
      HyperskillJoinCourseDialog(hyperskillCourse).show()
    }
    return true
  }

  // We have to use visible frame here because project is not yet created
  // See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
  private fun requestFocus() {
    val frame = WindowManager.getInstance().findVisibleFrame()
    if (frame is IdeFrame) {
      AppIcon.getInstance().requestFocus(frame)
    }
    frame.toFront()
  }

  override fun isAccessible(request: HttpRequest): Boolean = isHyperskillSupportAvailable()

  companion object {
    private const val EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill"
    private val OAUTH_CODE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/oauth\\?code=(\\w+)")
    private val OPEN_COURSE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME\\?stage_id=.+&project_id=.+")
    private val OPEN_STEP_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME\\?step_id=.+")
    private val PLUGIN_INFO = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/info")
  }

  private fun getIdeVersion(): String {
    val appInfo = ApplicationInfoImpl.getShadowInstance()
    return appInfo.versionName + " " + appInfo.fullVersion
  }

  data class PluginInfo(val version: String?, val edutools: String?)
}
