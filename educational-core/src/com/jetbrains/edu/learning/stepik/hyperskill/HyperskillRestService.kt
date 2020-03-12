package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
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
    if (OPEN_COURSE_PATTERN.matcher(uri).matches()) {
      return withHyperskillAuthorization { openStage(urlDecoder, request, context) }
    }

    if (OPEN_STEP_PATTERN.matcher(uri).matches()) {
      return withHyperskillAuthorization { openProblem(urlDecoder, request, context) }
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

  private fun withHyperskillAuthorization(action: () -> String?): String? {
    val account = HyperskillSettings.INSTANCE.account
    return if (account == null) {
      HyperskillConnector.getInstance().doAuthorize(Runnable { action() } )
      null
    }
    else {
      action()
    }
  }

  private fun openProblem(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stepId = getIntParameter("step_id", urlDecoder)
    val account = HyperskillSettings.INSTANCE.account ?: error("Attempt to open step for unauthorized user")
    val projectId = getSelectedProjectIdUnderProgress(account) ?: error("No selected project id")
    return openInIDE(projectId, null, stepId, request, context)
  }

  private fun openStage(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getIntParameter("stage_id", decoder)
    val projectId = getIntParameter("project_id", decoder)
    return openInIDE(projectId, stageId, null, request, context)
  }

  private fun openInIDE(projectId: Int, stageId: Int?, stepId: Int?, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    LOG.info("Opening ${EduNames.JBA} project: id=$projectId stageId=${stageId ?: "no stage"} stepId=${stepId ?: "no step"}")
    return when (val result = HyperskillProjectOpener.openProject(projectId, stageId, stepId)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("${EduNames.JBA} project opened: id=$projectId stageId=${stageId ?: "no stage"} stepId=${stepId ?: "no step"}")
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
