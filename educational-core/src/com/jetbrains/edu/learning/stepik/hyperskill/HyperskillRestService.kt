package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduNames.EDU_PREFIX
import com.jetbrains.edu.learning.authUtils.*
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillRestService.Companion.ReLoginDialogResult.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.*
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.annotations.NonNls
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
      sendPluginInfoResponse(request, context)
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

    if (hasOpenDialogs(EduNames.JBA)) {
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

    val reLoginAskResult = try {
      askToReLogin(userId)
    }
    catch (e: IllegalStateException) {
      LOG.error(e)
      return e.message
    }

    return when (reLoginAskResult) {
      YES -> {
        // logout
        HyperskillSettings.INSTANCE.account = null
        val messageBus = ApplicationManager.getApplication().messageBus
        messageBus.syncPublisher(HyperskillConnector.AUTHORIZATION_TOPIC).userLoggedOut()

        // login
        HyperskillConnector.getInstance().doAuthorize(Runnable { action() })
        null
      }
      NO -> action()
      CANCEL -> "Mismatching JetBrains Academy Accounts dialog has been canceled"
    }
  }

  private fun openProblem(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stepId = getIntParameter(STEP_ID, urlDecoder)
    val languageParameter = getStringParameter(LANGUAGE, urlDecoder)
    val language = languageParameter ?: getLanguageSelectedByUser().onError { error -> return error }
    val isLanguageSelectedByUser = languageParameter == null
    if (!HyperskillConnector.getInstance().isLoggedIn()) {
      error("Attempt to open step for unauthorized user")
    }
    val projectId = getSelectedProjectIdUnderProgress()
    if (projectId == null) {
      LOG.warn("Can't open project for step_id: $stepId language: $language")
      showError(SELECT_PROJECT)
      return SELECT_PROJECT
    }
    return openInIDE(HyperskillOpenStepRequest(projectId, stepId, language, isLanguageSelectedByUser), request, context)
  }

  private fun getLanguageSelectedByUser(): Result<String, String> {
    return invokeAndWaitIfNeeded {
      val dialog = HyperskillChooseLanguageDialog()
      if (!dialog.areLanguagesAvailable()) {
        showError(EduCoreBundle.message("hyperskill.error.no.supported.languages"))
        return@invokeAndWaitIfNeeded Err("No available languages to choose")
      }
      if (!dialog.showAndGet()) {
        return@invokeAndWaitIfNeeded Err("You should select language to open the problem")
      }
      Ok(dialog.selectedLanguage().requestLanguage)
    }
  }

  private fun openStage(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getIntParameter(STAGE_ID, decoder)
    val projectId = getIntParameter(PROJECT_ID, decoder)
    return openInIDE(HyperskillOpenStageRequest(projectId, stageId), request, context)
  }

  private fun askToReLogin(userId: Int): ReLoginDialogResult {
    val localAccount = HyperskillSettings.INSTANCE.account ?: error("Attempt to re-login unauthorized user")
    val browserAccount = HyperskillConnector.getInstance().getUser(userId).onError {
      error("Request to get user with $userId id is failed")
    }

    return getInEdt {
      requestFocus()

      val dialogResult = Messages.showDialog(
        "<html>${EduCoreBundle.message("hyperskill.accounts.are.different", localAccount.userInfo.getFullName(),
                                       browserAccount.fullname)}</html>",
        EduCoreBundle.message("hyperskill.accounts.are.different.title"),
        arrayOf(
          EduCoreBundle.message("hyperskill.accounts.are.different.re.login", browserAccount.fullname),
          EduCoreBundle.message("hyperskill.accounts.are.different.continue", localAccount.userInfo.getFullName())
        ),
        0,
        null
      )
      ReLoginDialogResult.valueOf(dialogResult)
    }
  }

  private fun openInIDE(openInProjectRequest: HyperskillOpenRequest,
                        request: FullHttpRequest,
                        context: ChannelHandlerContext): String? {
    LOG.info("Opening ${EduNames.JBA} project: $openInProjectRequest")
    return when (val result = ProjectOpener.getInstance().open(HyperskillOpenInIdeRequestHandler, openInProjectRequest)) {
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
    Notification("EduTools", EduNames.JBA, message, NotificationType.WARNING)
      .setListener(HSHyperlinkListener(false))
      .notify(null)
  }

  override fun isAccessible(request: HttpRequest): Boolean = isHyperskillSupportAvailable()

  companion object {
    // Parameters
    @NonNls
    private const val CODE: String = "code"

    @NonNls
    private const val LANGUAGE: String = "language"

    @NonNls
    private const val PROJECT_ID: String = "project_id"

    @NonNls
    private const val STAGE_ID: String = "stage_id"

    @NonNls
    private const val STEP_ID: String = "step_id"

    @NonNls
    private const val USER_ID: String = "user_id"

    const val EDU_HYPERSKILL_SERVICE_NAME: String = "$EDU_PREFIX/hyperskill"
    private val OAUTH_CODE_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME/oauth\?$CODE=(\w+)""")
    private val OPEN_COURSE_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME\?$STAGE_ID=.+&$PROJECT_ID=.+""")
    private val OPEN_STEP_PATTERN = Pattern.compile("""/api/$EDU_HYPERSKILL_SERVICE_NAME\?$STEP_ID=.+""")
    private val PLUGIN_INFO = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME/info")

    private enum class ReLoginDialogResult(private val result: Int) {
      YES(0), NO(1), CANCEL(-1);

      companion object {
        fun valueOf(value: Int): ReLoginDialogResult = values().find { it.result == value } ?: error("Unexpected value: $value")
      }
    }
  }
}
