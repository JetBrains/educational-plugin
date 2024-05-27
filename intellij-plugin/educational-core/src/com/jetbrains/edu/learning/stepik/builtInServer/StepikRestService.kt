package com.jetbrains.edu.learning.stepik.builtInServer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.intellij.util.io.origin
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.authUtils.createResponse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikNames.getStepikUrl
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.io.send
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class StepikRestService : OAuthRestService(StepikNames.STEPIK) {
  override val isPrefixlessAllowed: Boolean = true

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(
    request: FullHttpRequest,
    urlDecoder: QueryStringDecoder
  ): Boolean {
    return if (request.method() === HttpMethod.GET
               // If isOriginAllowed is `false` check if it is a valid oAuth request with empty origin
               && ((isOriginAllowed(request) === OriginCheckResult.ALLOW || StepikConnector.getInstance().isValidOAuthRequest(request, urlDecoder)))) {
      true
    }
    else {
      super.isHostTrusted(request, urlDecoder)
    }
  }

  @Throws(IOException::class)
  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val uri = urlDecoder.uri()
    LOG.info("Request: $uri")
    val errorCodeMatcher = oauthErrorCodePattern.matcher(uri)
    if (errorCodeMatcher.matches()) {
      val pageContent = getInternalTemplateText("stepik.redirectPage.html")
      createResponse(pageContent).send(context.channel(), request)
      return null
    }
    val codeMatcher = oauthCodePattern.matcher(uri)
    if (codeMatcher.matches()) {
      val code = getStringParameter(CODE_ARGUMENT, urlDecoder)
      if (code != null) {
        val success = StepikConnector.getInstance().login(code)
        val user = EduSettings.getInstance().user
        if (success && user != null) {
          showOkPage(request, context)
          @Suppress("HardCodedStringLiteral")
          EduNotificationManager.showInfoNotification(
            title = StepikNames.STEPIK,
            content = "Logged in as ${user.firstName} ${user.lastName}"
          )
          focusOnApplicationWindow()
          return null
        }
      }
      @Suppress("HardCodedStringLiteral")
      EduNotificationManager.showErrorNotification(title = StepikNames.STEPIK, content = "Failed to log in")
      return sendErrorResponse(request, context, "Couldn't find code parameter for Stepik OAuth")
    }
    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    val message = "Unknown command: $uri"
    LOG.info(message)
    return message
  }

  override fun getServiceName(): String = StepikConnector.getInstance().serviceName

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      return if (origin == getStepikUrl()) OriginCheckResult.ALLOW
      else OriginCheckResult.ASK_CONFIRMATION
    }
    return originAllowed
  }

  private fun focusOnApplicationWindow() {
    val frame = WindowManager.getInstance().findVisibleFrame() ?: return
    ApplicationManager.getApplication().invokeLater {
      AppIcon.getInstance().requestFocus(frame as IdeFrame)
      frame.toFront()
    }
  }

  private val oauthCodePattern = StepikConnector.getInstance().getOAuthPattern()

  private val oauthErrorCodePattern = StepikConnector.getInstance().getOAuthPattern("\\?error=(\\w+)")

  companion object {
    private val LOG = logger<StepikRestService>()
  }
}
