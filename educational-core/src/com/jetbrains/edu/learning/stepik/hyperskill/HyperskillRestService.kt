package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.*
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
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val openCourseMatcher = OPEN_COURSE_PATTERN.matcher(uri)
    val openStepMatcher = OPEN_STEP_PATTERN.matcher(uri)
    val pluginInfo = PLUGIN_INFO.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || openCourseMatcher.matches() || openStepMatcher.matches() ||
                                                       pluginInfo.matches())) {
      true
    }
    else super.isHostTrusted(request, urlDecoder)
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

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = getStringParameter(CODE, urlDecoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.getInstance().login(code)
      if (success) {
        LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = getInternalTemplateText("hyperskill.redirectPage.html")
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    val hasOpenDialogs = getInEdt(modalityState = ModalityState.any()) {
      if (ModalityState.current() != ModalityState.NON_MODAL) {
        HyperskillProjectOpener.requestFocus()
        Messages.showInfoMessage(EduCoreBundle.message("hyperskill.rest.service.modal.dialogs.message"),
                                 EduCoreBundle.message("hyperskill.rest.service.modal.dialogs.title"))
        return@getInEdt false
      }
      true
    }

    if (!hasOpenDialogs) {
      sendOk(request, context)
      return null
    }

    if (OPEN_COURSE_PATTERN.matcher(uri).matches()) {
      val userId = getIntParameter(USER_ID, urlDecoder)
      return withHyperskillAuthorization(userId) { openStage(urlDecoder, request, context) }
    }

    if (OPEN_STEP_PATTERN.matcher(uri).matches()) {
      val userId = getIntParameter(USER_ID, urlDecoder)
      return withHyperskillAuthorization(userId) { openProblem(urlDecoder, request, context) }
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  private fun withHyperskillAuthorization(userId: Int, action: () -> String?): String? {
    val account = HyperskillSettings.INSTANCE.account
    return if (account == null) {
      HyperskillConnector.getInstance().doAuthorize(Runnable { action() })
      null
    }
    else {
      withUserIdCheck(userId) { action() }
    }
  }

  private fun withUserIdCheck(userId: Int, action: () -> String?): String? {
    if (userId == -1) return action()

    val localAccount = HyperskillSettings.INSTANCE.account
    if (localAccount == null) {
      val message = "Attempt to verify unauthorized user"
      LOG.warn(message)
      return message
    }
    if (localAccount.userInfo.id == userId) {
      return action()
    }

    val reLogin = try {
      askToReLogin(userId)
    }
    catch (e: IllegalStateException) {
      LOG.error(e)
      return e.message
    }
    if (!reLogin) return action()

    // logout
    HyperskillSettings.INSTANCE.account = null
    val messageBus = ApplicationManager.getApplication().messageBus
    messageBus.syncPublisher<EduLogInListener>(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()

    // login
    HyperskillConnector.getInstance().doAuthorize(Runnable { action() })
    return null
  }

  private fun openProblem(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stepId = getIntParameter(STEP_ID, urlDecoder)
    val language = getStringParameter("language", urlDecoder) ?: error("No language for open step request")
    val account = HyperskillSettings.INSTANCE.account ?: error("Attempt to open step for unauthorized user")
    val projectId = getSelectedProjectIdUnderProgress(account)
    if (projectId == null) {
      showError(SELECT_PROJECT)
      return SELECT_PROJECT
    }
    return openInIDE(HyperskillOpenStepRequest(projectId, stepId, language), request, context)
  }

  private fun openStage(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getIntParameter(STAGE_ID, decoder)
    val projectId = getIntParameter(PROJECT_ID, decoder)
    return openInIDE(HyperskillOpenStageRequest(projectId, stageId), request, context)
  }

  private fun askToReLogin(userId: Int): Boolean {
    val localAccount = HyperskillSettings.INSTANCE.account ?: error("Attempt to re-login unauthorized user")
    val browserAccount = HyperskillConnector.getInstance().getUser(userId).onError {
      error("Request to get user with $userId id is failed")
    }

    return getInEdt {
      Messages.showOkCancelDialog(
        "<html>${EduCoreBundle.message("hyperskill.accounts.are.different", localAccount.userInfo.fullname,
                                       browserAccount.fullname)}</html>",
        EduCoreBundle.message("hyperskill.accounts.are.different.title"),
        EduCoreBundle.message("hyperskill.accounts.are.different.re.login", browserAccount.fullname),
        EduCoreBundle.message("hyperskill.accounts.are.different.continue", localAccount.userInfo.fullname),
        null
      ) == Messages.OK
    }
  }

  private fun openInIDE(openInProjectRequest: HyperskillOpenInProjectRequest,
                        request: FullHttpRequest,
                        context: ChannelHandlerContext): String? {
    LOG.info("Opening ${EduNames.JBA} project: $openInProjectRequest")
    return when (val result = HyperskillProjectOpener.open(openInProjectRequest)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("${EduNames.JBA} project opened: $openInProjectRequest")
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
    Notification(HYPERSKILL, EduNames.JBA, message, NotificationType.WARNING,
                 HSHyperlinkListener(false)).notify(null)
  }

  override fun isAccessible(request: HttpRequest): Boolean = isHyperskillSupportAvailable()

  companion object {
    // Parameters
    private const val CODE = "code"
    private const val PROJECT_ID = "project_id"
    private const val STAGE_ID = "stage_id"
    private const val STEP_ID = "step_id"
    private const val USER_ID = "user_id"

    const val EDU_HYPERSKILL_SERVICE_NAME: String = "edu/hyperskill"
    private val OAUTH_CODE_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME/oauth\?$CODE=(\w+)""")
    private val OPEN_COURSE_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME\?$STAGE_ID=.+&$PROJECT_ID=.+""")
    private val OPEN_STEP_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME\?$STEP_ID=.+""")
    private val PLUGIN_INFO = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/info")
  }

  private fun getIdeVersion(): String {
    val appInfo = ApplicationInfoImpl.getShadowInstance()
    return appInfo.versionName + " " + appInfo.fullVersion
  }

  data class PluginInfo(val version: String?, val edutools: String?)
}
