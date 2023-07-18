package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.api.EduLoginConnector.Companion.STATE
import com.jetbrains.edu.learning.authUtils.*
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult
import com.jetbrains.edu.learning.courseFormat.ext.ValidationErrorMessage
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.notificationFromCourseValidation
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

class HyperskillRestService : OAuthRestService(HYPERSKILL) {

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    val uri = request.uri()
    val oAuthCodeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val oAuthErrorMatcher = OAUTH_ERROR_CODE_PATTERN.matcher(uri)
    val openCourseMatcher = OPEN_COURSE_PATTERN.matcher(uri)
    val openStepMatcher = OPEN_STEP_PATTERN.matcher(uri)
    val pluginInfo = PLUGIN_INFO.matcher(uri)
    return if (request.method() === HttpMethod.GET && (oAuthCodeMatcher.matches()
                                                       || oAuthErrorMatcher.matches()
                                                       || openCourseMatcher.matches()
                                                       || openStepMatcher.matches()
                                                       || pluginInfo.matches())) {
      true
    }
    else {
      super.isHostTrusted(request, urlDecoder)
    }
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

    if (OAUTH_ERROR_CODE_PATTERN.matcher(uri).matches()) {
      return sendErrorResponse(request, context, "Failed to login")
    }

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = getStringParameter(CODE_ARGUMENT, urlDecoder)!! // cannot be null because of pattern
      val receivedState = getStringParameter(STATE, urlDecoder) ?: return sendErrorResponse(
        request,
        context,
        "State param was not received."
      )

      val success = HyperskillConnector.getInstance().login(code, receivedState)
      if (success) {
        LOG.info("$platformName: OAuth code is handled")
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

  override fun getServiceName(): String = HyperskillConnector.getInstance().serviceName

  private fun withHyperskillAuthorization(userId: Int, action: () -> String?): String? {
    val connector = HyperskillConnector.getInstance()
    return if (!connector.isLoggedIn()) {
      connector.doAuthorize(Runnable { action() })
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

        // login
        HyperskillConnector.getInstance().doAuthorize(Runnable { action() })
        null
      }
      NO -> action()
      CANCEL -> "Mismatching Hyperskill Accounts dialog has been canceled"
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
    val projectId = getSelectedProjectIdUnderProgress() ?: return openInIDE(
      HyperskillOpenStepRequest(
        stepId,
        language,
        isLanguageSelectedByUser
      ), request, context
    )
    return openInIDE(HyperskillOpenStepWithProjectRequest(projectId, stepId, language, isLanguageSelectedByUser), request, context)
  }

  private fun getLanguageSelectedByUser(): Result<String, String> {
    return invokeAndWaitIfNeeded {
      val dialog = HyperskillChooseLanguageDialog()
      if (!dialog.areLanguagesAvailable()) {
        showError(ValidationErrorMessage(EduCoreBundle.message("hyperskill.error.no.supported.languages")))
        return@invokeAndWaitIfNeeded Err("No available languages to choose")
      }
      if (!dialog.showAndGet()) {
        return@invokeAndWaitIfNeeded Err("You should select language to open the problem")
      }
      val requestLanguage = dialog.selectedLanguage().requestLanguage
      Ok(requestLanguage)
    }
  }

  private fun openStage(decoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val stageId = getIntParameter(STAGE_ID, decoder)
    val projectId = getIntParameter(PROJECT_ID, decoder)
    return openInIDE(HyperskillOpenProjectStageRequest(projectId, stageId), request, context)
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
        val validationResult = result.error
        val message = validationResult.message
        LOG.warn(message)
        showError(validationResult)
        sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
        message
      }
    }
  }

  private fun showError(validationResult: CourseValidationResult) {
    notificationFromCourseValidation(validationResult, EduNames.JBA).notify(null)
  }

  override fun isAccessible(request: HttpRequest): Boolean = isHyperskillSupportAvailable()

  companion object {
    // Parameters
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

    private val OAUTH_CODE_PATTERN = HyperskillConnector.getInstance().getOAuthPattern()
    private val OAUTH_ERROR_CODE_PATTERN = HyperskillConnector.getInstance().getOAuthPattern("\\?error=(\\w+)")
    private val OPEN_COURSE_PATTERN = HyperskillConnector.getInstance().getServicePattern("""\?$STAGE_ID=.+&$PROJECT_ID=.+""")
    private val OPEN_STEP_PATTERN = HyperskillConnector.getInstance().getServicePattern("""\?$STEP_ID=.+""")
    private val PLUGIN_INFO = HyperskillConnector.getInstance().getServicePattern("/info")

    private enum class ReLoginDialogResult(private val result: Int) {
      YES(0), NO(1), CANCEL(-1);

      companion object {
        fun valueOf(value: Int): ReLoginDialogResult = values().find { it.result == value } ?: error("Unexpected value: $value")
      }
    }
  }
}
