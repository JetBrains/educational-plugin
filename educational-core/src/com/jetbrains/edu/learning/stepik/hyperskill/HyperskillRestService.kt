package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.newproject.ui.CoursePanel.nameToLocation
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HSHyperlinkListener
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
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
      return if (account == null) {
        HyperskillConnector.getInstance().doAuthorize(Runnable { openProject(urlDecoder, request, context) })
        null
      }
      else {
        openProject(urlDecoder, request, context)
      }
    }

    val openStepMatcher = OPEN_STEP_PATTERN.matcher(uri)
    if (openStepMatcher.matches()) {
      val account = HyperskillSettings.INSTANCE.account
      return if (account == null) {
        HyperskillConnector.getInstance().doAuthorize(Runnable { openStep(urlDecoder, request, context) })
        null
      }
      else {
        openStep(urlDecoder, request, context)
      }
    }

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = getStringParameter("code", urlDecoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.getInstance().login(code)
      if (success) {
        LOG.info("$myPlatformName: OAuth code is handled")
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
    val account = HyperskillSettings.INSTANCE.account ?: error("Attempt to open step for unauthorized user")
    val projectId = getSelectedProjectIdUnderProgress(account) ?: error("No selected project id")

//    val (project, course) = openOrCreateProject(projectId) ?: return sendErrorResponse(request, context,
//                                                                                       "Failed to create or open ${EduNames.JBA} project")
//    runInEdt { HyperskillProjectOpener.requestFocus() }
//
//    if (course !is HyperskillCourse) return sendErrorResponse(request, context, "Failed to create or open ${EduNames.JBA} project")
//
//    val lesson = HyperskillProjectOpener.findOrCreateProblemsLesson(course, project)
//    val lessonDir = lesson.getLessonDir(project)
//                    ?: return sendErrorResponse(request, context, "Could not find Problems directory")
//
//    val stepSource = ProgressManager.getInstance().run(
//      object : Task.WithResult<HyperskillStepSource?, Exception>(null, "Loading ${EduNames.JBA} problem", true) {
//        override fun compute(indicator: ProgressIndicator): HyperskillStepSource? {
//          return HyperskillConnector.getInstance().getStepSource(stepId)
//        }
//      }) ?: return sendErrorResponse(request, context, "Could not find get step source for the task")
//
//    val task = HyperskillProjectOpener.findOrCreateTask(course, lesson, stepSource, lessonDir, project)
//    runInEdt {
//      NavigationUtils.navigateToTask(project, task)
//      HyperskillProjectOpener.requestFocus()
//    }
    return when (val result = HyperskillProjectOpener.openProject(projectId, stepId = stepId)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("${EduNames.JBA} project opened: $projectId")
        null
      }
      is Err -> {
        val message = result.error
        LOG.warn(message)
        showError(message)
        sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
        message
      }
    }
  }

  private fun openOrCreateProject(projectId: Int?): Pair<Project, Course>? {
    var projectCourse = EduBuiltInServerUtils.focusOpenProject {
      it is HyperskillCourse && projectId != null && it.hyperskillProject?.id == projectId
    }
    if (projectCourse != null) return projectCourse
    projectCourse = EduBuiltInServerUtils.openRecentProject {
      it is HyperskillCourse && projectId != null && it.hyperskillProject?.id == projectId
    }
    if (projectCourse != null) return projectCourse
    return createProject(projectId)
  }

  private fun openProject(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getStringParameter("stage_id", decoder)?.toInt() ?: return "The stage_id parameter was not found"
    val projectId = getStringParameter("project_id", decoder)?.toInt() ?: return "The project_id parameter was not found"
    LOG.info("Opening a stage $stageId from project $projectId")

    return when (val result = HyperskillProjectOpener.openProject(projectId, stageId)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("${EduNames.JBA} project opened: $projectId")
        null
      }
      is Err -> {
        val message = result.error
        LOG.warn(message)
        showError(message)
        sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
        message
      }
    }
  }

  private fun showError(message: String) {
    Notification(HYPERSKILL, HYPERSKILL, message, NotificationType.WARNING,
                 HSHyperlinkListener(false)).notify(null)
  }

  private fun createProject(projectId: Int?): Pair<Project, Course>? {
    if (projectId == null) {
      return null
    }

    return when (val result = HyperskillProjectOpener.getHyperskillCourseUnderProgress(projectId, null, null)) {
      is Err -> {
        showError(result.error)
        null
      }
      is Ok -> {
        val hyperskillCourse = result.value
        val project = createProjectFromCourse(hyperskillCourse) ?: return null
        return Pair(project, hyperskillCourse)
      }
    }
  }

  private fun createProjectFromCourse(hyperskillCourse: HyperskillCourse): Project? {
    val configurator = hyperskillCourse.configurator ?: return null
    val projectGenerator = configurator.courseBuilder.getCourseProjectGenerator(hyperskillCourse) ?: return null
    val location = nameToLocation(hyperskillCourse)

    var project: Project? = null
    ApplicationManager.getApplication().invokeAndWait {
      TransactionGuard.getInstance().submitTransactionAndWait {
        val settings = configurator.courseBuilder.getLanguageSettings().settings
                       ?: error("Settings should not be null while creating the project")
        project = projectGenerator.doCreateCourseProject(location, settings)
      }
    }
    return project
  }

  override fun isAccessible(request: HttpRequest): Boolean = isHyperskillSupportAvailable()

  companion object {
    const val EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill"
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
