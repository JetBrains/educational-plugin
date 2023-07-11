package com.jetbrains.edu.learning.stepik.builtInServer

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.api.EduLoginConnector.Companion.STATE
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.authUtils.createResponse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
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
    val uri = request.uri()
    val codeMatcher = oauthCodePattern.matcher(uri)
    val errorMatcher = oauthErrorCodePattern.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || errorMatcher.matches())) {
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

      val receivedState = getStringParameter(STATE, urlDecoder) ?: sendErrorResponse(
        request,
        context,
        "State param was not received."
      )

      if (code != null) {
        val success = StepikConnector.getInstance().login(code, receivedState)
        val user = EduSettings.getInstance().user
        if (success && user != null) {
          showOkPage(request, context)
          showStepikNotification(NotificationType.INFORMATION, "Logged in as " + user.firstName + " " + user.lastName)
          focusOnApplicationWindow()
          return null
        }
      }
      showStepikNotification(NotificationType.ERROR, "Failed to log in")
      return sendErrorResponse(request, context, "Couldn't find code parameter for Stepik OAuth")
    }
    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    val message = "Unknown command: $uri"
    LOG.info(message)
    return message
  }

  override fun getServiceName(): String = StepikConnector.getInstance().serviceName

  private fun focusOnApplicationWindow() {
    val frame = WindowManager.getInstance().findVisibleFrame() ?: return
    ApplicationManager.getApplication().invokeLater {
      AppIcon.getInstance().requestFocus(frame as IdeFrame)
      frame.toFront()
    }
  }

  private fun showStepikNotification(notificationType: NotificationType, text: String) {
    val notification = Notification("JetBrains Academy", StepikNames.STEPIK, text, notificationType)
    notification.notify(null)
  }

  private val oauthCodePattern = StepikConnector.getInstance().getOAuthPattern()

  private val oauthErrorCodePattern = StepikConnector.getInstance().getOAuthPattern("\\?error=(\\w+)")

  companion object {
    private val LOG = logger<StepikRestService>()
  }
}
